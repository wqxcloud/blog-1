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
package me.qyh.blog.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.qyh.blog.ui.ExportPage;

public class ImportResult {

	// 原始页面，用于恢复
	private List<ExportPage> oldPages = new ArrayList<>();
	// 导入过程中出错
	private List<ImportError> errors = new ArrayList<>();
	// 导入成功的页面序号
	private List<ImportSuccess> successes = new ArrayList<>();

	public List<ExportPage> getOldPages() {
		return oldPages;
	}

	public void setOldPages(List<ExportPage> oldPages) {
		this.oldPages = oldPages;
	}

	public List<ImportError> getErrors() {
		return errors;
	}

	public void setErrors(List<ImportError> errors) {
		this.errors = errors;
	}

	public List<ImportSuccess> getSuccesses() {
		return successes;
	}

	public void setSuccesses(List<ImportSuccess> successes) {
		this.successes = successes;
	}

	public void addOldPage(ExportPage old) {
		oldPages.add(old);
	}

	public void addError(ImportError error) {
		errors.add(error);
	}

	public void addSuccess(ImportSuccess success) {
		successes.add(success);
	}

	public void addErrors(List<ImportError> errors) {
		this.errors.addAll(errors);
	}
	
	public void sort(){
		Collections.sort(errors);
	}

}
