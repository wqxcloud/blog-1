package me.qyh.blog.file.oss;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.BucketManager.Batch;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.Resize;
import me.qyh.blog.service.FileService;
import me.qyh.util.UrlUtils;
import me.qyh.util.Validators;

/**
 * 提供了对七牛云存储的简单操作，必须引入七牛云的sdk:
 * {@link http://developer.qiniu.com/code/v7/sdk/java.html}
 * <p>
 * 如果提供了backupAbsPath，那么上传时同时也会将文件备份至该目录下，通过new File(backAbsPath,key)可以定位备份文件
 * </p>
 * <p>
 * 如果空间为私有空间，请设置secret为true，这样文件的路径将会增加必要的token信息
 * </p>
 * 
 * @author Administrator
 *
 */
public class QiniuFileStore extends AbstractOssFileStore {

	private static final long PRIVATE_DOWNLOAD_URL_EXPIRES = 3600L;

	private String ak;// ACCESS_KEY
	private String sk;// SECRET_KEY
	private String urlPrefix;// 外链域名
	private String bucket;
	private boolean secret;// 私人空间
	private long privateDownloadUrlExpires = PRIVATE_DOWNLOAD_URL_EXPIRES;
	private Character styleSplitChar;// 样式分隔符
	private boolean sourceProtected;// 原图保护
	private String style;// 样式

	/**
	 * 所允许的样式分割符号
	 * 
	 * @see https://portal.qiniu.com/bucket/qyhqym/separator
	 */
	private static final char[] ALLOW_STYLE_SPLIT_CHARS = { '-', '_', '!', '/', '~', '`', '@', '$', '^', '&', '*', '(',
			')', '+', '=', '{', '}', '[', ']', '|', ':', ';', '\"', '\'', '<', '>', ',', '.' };

	/**
	 * 跟七牛云设置有关
	 */
	private Resize defaultResize;

	private Auth auth;

	private static final int FILE_NOT_EXISTS_ERROR_CODE = 612;// 文件不存在错误码

	@Override
	protected void upload(String key, File file) throws UploadException {
		UploadManager uploadManager = new UploadManager();
		// 是否不抛出异常就代表着上传成功了？
		try {
			uploadManager.put(file, key, getUpToken());
		} catch (QiniuException e) {
			Response r = e.response;
			try {
				throw new UploadException("七牛云上传失败，异常信息:" + r.toString() + ",相应信息:" + r.bodyString(), e);
			} catch (QiniuException e1) {
				// ignore;
			}
		}
	}

	@Override
	protected boolean _delete(String key) {
		boolean flag = false;
		BucketManager bucketManager = new BucketManager(auth);
		// 要测试的空间和key，并且这个key在你空间中存在
		try {
			// 调用delete方法移动文件
			bucketManager.delete(bucket, key);
			flag = true;
		} catch (QiniuException e) {
			Response r = e.response;
			if (r.statusCode == FILE_NOT_EXISTS_ERROR_CODE) {
				flag = true;
			}
			try {
				logger.error("七牛云删除失败，异常信息:" + r.toString() + ",相应信息:" + r.bodyString());
			} catch (QiniuException e1) {
				// ignore;
			}
		}
		return flag;
	}

	@Override
	public String getUrl(String key) {
		String url = urlPrefix + key;
		if (secret) {
			return auth.privateDownloadUrl(url);
		}
		if (image(key) && sourceProtected)
			return url + styleSplitChar + style;
		return url;
	}

	// http://7xst8w.com1.z0.glb.clouddn.com/my-java.png?attname=
	@Override
	public String getDownloadUrl(String key) {
		String url = urlPrefix + key + "?attname=";
		if (secret || sourceProtected) {
			return auth.privateDownloadUrl(url);
		}
		return url;
	}

	@Override
	public String getPreviewUrl(String key) {
		if (image(key)) {
			String param = buildResizeParam();
			String url = urlPrefix + key + (param == null ? "" : "?" + param);
			if (secret) {
				return auth.privateDownloadUrl(url);
			} else if (sourceProtected) {
				// 只能采用样式访问
				return urlPrefix + key + styleSplitChar + style;
			} else {
				return url;
			}
		} else {
			return null;
		}
	}

	private static final int RECOMMEND_LIMIT = 100;

	@Override
	public boolean _deleteBatch(String key) {
		try {
			List<String> keys = new ArrayList<String>();
			BucketManager bucketManager = new BucketManager(auth);
			FileListing fileListing = bucketManager.listFiles(bucket, key + FileService.SPLIT_CHAR, null,
					RECOMMEND_LIMIT, null);

			do {
				FileInfo[] items = fileListing.items;
				if (items != null && items != null) {
					for (FileInfo fileInfo : items)
						keys.add(fileInfo.key);
				}
				fileListing = bucketManager.listFiles(bucket, key + FileService.SPLIT_CHAR, fileListing.marker,
						RECOMMEND_LIMIT, null);
			} while (!fileListing.isEOF());

			if (keys.isEmpty())
				return true;

			Batch batch = new Batch();
			batch.delete(bucket, keys.toArray(new String[] {}));
			return bucketManager.batch(batch).isOK();
		} catch (QiniuException e) {
			// 捕获异常信息
			Response r = e.response;
			logger.error(r.toString());
		}
		return false;
	}

	private boolean allowStyleSplitChar(char schar) {
		for (char ch : ALLOW_STYLE_SPLIT_CHARS)
			if (ch == schar)
				return true;
		return false;

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		if (Validators.isEmptyOrNull(ak, true)) {
			throw new SystemException("AccessKey不能为空");
		}
		if (Validators.isEmptyOrNull(sk, true)) {
			throw new SystemException("SecretKey不能为空");
		}
		if (Validators.isEmptyOrNull(bucket, true)) {
			throw new SystemException("Bucket不能为空");
		}
		if (Validators.isEmptyOrNull(urlPrefix, true)) {
			throw new SystemException("外链域名不能为空");
		}
		if (!UrlUtils.isAbsoluteUrl(urlPrefix)) {
			throw new SystemException("外链域名必须是一个绝对路径");
		}
		if (!urlPrefix.endsWith("/")) {
			urlPrefix += "/";
		}
		auth = Auth.create(ak, sk);

		if (privateDownloadUrlExpires < PRIVATE_DOWNLOAD_URL_EXPIRES) {
			privateDownloadUrlExpires = PRIVATE_DOWNLOAD_URL_EXPIRES;
		}

		if (sourceProtected) {
			if (style == null)
				throw new SystemException("开启了原图保护之后请指定一个默认的样式名");
			if (styleSplitChar == null)
				styleSplitChar = ALLOW_STYLE_SPLIT_CHARS[0];
			if (!allowStyleSplitChar(styleSplitChar)) {
				StringBuilder sb = new StringBuilder();
				for (char ch : ALLOW_STYLE_SPLIT_CHARS)
					sb.append(ch).append(',');
				sb.deleteCharAt(sb.length() - 1);
				throw new SystemException("样式分隔符不被接受，样式分割符必须为以下字符:" + sb.toString());
			}
		}

	}

	/**
	 * {@link Resize#isKeepRatio()}设置无效
	 * 
	 * @return
	 */
	protected String buildResizeParam() {
		if (defaultResize == null) {
			return null;
		} else {
			if (defaultResize.getSize() != null) {
				return "imageView2/2/w/" + defaultResize.getSize() + "/h/" + defaultResize.getSize();
			}
			if (defaultResize.getWidth() == 0 && defaultResize.getHeight() == 0) {
				return null;
			}
			if (defaultResize.getWidth() == 0) {
				return "imageView2/2/h/" + defaultResize.getHeight();
			}
			if (defaultResize.getHeight() == 0) {
				return "imageView2/2/w/" + defaultResize.getWidth();
			}
			return "imageView2/2/w/" + defaultResize.getWidth() + "/h/" + defaultResize.getHeight();
		}
	}

	// 简单上传，使用默认策略，只需要设置上传的空间名就可以了
	protected String getUpToken() {
		return auth.uploadToken(bucket);
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public void setAk(String ak) {
		this.ak = ak;
	}

	public void setSk(String sk) {
		this.sk = sk;
	}

	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}

	public void setSecret(boolean secret) {
		this.secret = secret;
	}

	public void setDefaultResize(Resize defaultResize) {
		this.defaultResize = defaultResize;
	}

	public void setPrivateDownloadUrlExpires(long privateDownloadUrlExpires) {
		this.privateDownloadUrlExpires = privateDownloadUrlExpires;
	}

	public void setStyleSplitChar(Character styleSplitChar) {
		this.styleSplitChar = styleSplitChar;
	}

	public void setSourceProtected(boolean sourceProtected) {
		this.sourceProtected = sourceProtected;
	}

	public void setStyle(String style) {
		this.style = style;
	}

}
