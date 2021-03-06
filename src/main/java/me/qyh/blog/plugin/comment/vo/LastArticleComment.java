/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.plugin.comment.vo;

import me.qyh.blog.core.entity.Article;
import me.qyh.blog.plugin.comment.entity.Comment;

/**
 * 最近的文章评论
 * <p>在查询最近的文章评论的时候，如果文章是受保护的，那么评论也应该是受保护的</p>
 */
public class LastArticleComment extends Comment{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Article article;

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}
	
}
