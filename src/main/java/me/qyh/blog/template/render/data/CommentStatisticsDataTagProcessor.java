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
package me.qyh.blog.template.render.data;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.CommentServer;
import me.qyh.blog.core.vo.CommentStatistics;

public class CommentStatisticsDataTagProcessor extends DataTagProcessor<CommentStatistics> {

	@Autowired(required = false)
	private CommentServer commentServer;

	public CommentStatisticsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected CommentStatistics query(Attributes attributes) throws LogicException {
		if (commentServer == null) {
			return new CommentStatistics();
		}
		return commentServer.queryCommentStatistics(Environment.getSpace());
	}

	@Override
	public List<String> getAttributes() {
		return List.of();
	}

}
