/*
 * Copyright 2018 qyh.me
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
package me.qyh.blog.template.render.thymeleaf.dialect;

import java.io.IOException;
import java.io.Writer;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.FastStringWriter;

import me.qyh.blog.core.text.CommonMarkdown2Html;
import me.qyh.blog.core.text.Markdown2Html;

/**
 * 能够使用 markdown标签
 * <p>
 * <b>markdown标签内的标签将不会被解析！！，如果需要获取数据，需要采用内联表达式</b>
 * </p>
 * 
 * @since 5.9
 * @author wwwqyhme
 *
 */
public class MarkdownModelProcessor extends AbstractElementModelProcessor {

	private static final int PRECEDENCE = 1000;

	private Markdown2Html markdown2Html;

	public MarkdownModelProcessor(String dialectPrefix, ApplicationContext ctx) {
		super(TemplateMode.HTML, dialectPrefix, "markdown", false, null, false, PRECEDENCE);
		try {
			this.markdown2Html = ctx.getBean(Markdown2Html.class);
		} catch (BeansException e) {
			this.markdown2Html = CommonMarkdown2Html.INSTANCE;
		}
	}

	@Override
	protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
		boolean reset = false;
		try {
			if (removeOpenTag(model)) {
				try (Writer writer = new FastStringWriter()) {
					model.write(writer);
					model.reset();
					reset = true;

					model.add(context.getModelFactory().createText(markdown2Html.toHtml(writer.toString())));

				} catch (IOException e) {
					throw new TemplateProcessingException(e.getMessage(), e);
				}
			}
		} finally {
			if (!reset)
				model.reset();
		}
	}

	protected boolean removeOpenTag(IModel model) {
		int size = model.size();
		if (size == 1) {
			// <markdown/>
			model.reset();
			return false;
		} else {
			ITemplateEvent last = model.get(size - 1);
			if (last instanceof ICloseElementTag) {
				model.remove(size - 1);
			} else {
				return false;
			}
			model.remove(0);// remove <markdown>
			return true;
		}
	}
}
