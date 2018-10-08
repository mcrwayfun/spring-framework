/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.springframework.core.NestedIOException;
import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Convenience base class for {@link Resource} implementations,
 * pre-implementing typical behavior.
 *
 * <p>The "exists" method will check whether a File or InputStream can
 * be opened; "isOpen" will always return false; "getURL" and "getFile"
 * throw an exception; and "toString" will return the description.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 */
public abstract class AbstractResource implements Resource {

	/**
	 * 判断文件是否存在，若判断过程产生异常（因为会调用SecurityManager来判断），就关闭对应的流
	 */
	@Override
	public boolean exists() {
		// Try file existence: can we find the file in the file system?
		try {
			return getFile().exists();
		}
		catch (IOException ex) {
			// Fall back to stream existence: can we open the stream?
			try {
				// 关闭对应的流
				getInputStream().close();
				return true;
			}
			catch (Throwable isEx) {
				return false;
			}
		}
	}

	/**
	 * 文件存在即可读
	 */
	@Override
	public boolean isReadable() {
		return exists();
	}

	/**
	 * 直接返回false，表示未打开
	 */
	@Override
	public boolean isOpen() {
		return false;
	}

	/**
	 * 直接返回false，表示不为 file
	 */
	@Override
	public boolean isFile() {
		return false;
	}

	/**
	 * 抛出 FileNotFoundException 异常，交给子类实现？
	 * TODO-mcrwayfun 子类如何实现？
	 */
	@Override
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}

	/**
	 * 基于 getURL() 返回的 URL 构建 URI
	 */
	@Override
	public URI getURI() throws IOException {
		URL url = getURL();
		try {
			return ResourceUtils.toURI(url);
		}
		catch (URISyntaxException ex) {
			throw new NestedIOException("Invalid URI [" + url + "]", ex);
		}
	}

	/**
	 * 抛出 FileNotFoundException 异常，交给子类实现
	 */
	@Override
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}

	/**
	 * 根据 getInputStream() 的返回结果来构建 ReadableByteChannel
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 返回资源的长度
	 *
	 * 这个资源长度实际上是资源字节的长度，通过读取整个流来计算字节size
	 */
	@Override
	public long contentLength() throws IOException {
		InputStream is = getInputStream();
		try {
			long size = 0;
			byte[] buf = new byte[255];
			int read;
			while ((read = is.read(buf)) != -1) {
				size += read;
			}
			return size;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ex) {
			}
		}
	}

	/**
	 * 返回资源最后的修改时间
	 */
	@Override
	public long lastModified() throws IOException {
		long lastModified = getFileForLastModifiedCheck().lastModified();
		if (lastModified == 0L) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for resolving its last-modified timestamp");
		}
		return lastModified;
	}

	/**
	 * Determine the File to use for timestamp checking.
	 * <p>The default implementation delegates to {@link #getFile()}.
	 * @return the File to use for timestamp checking (never {@code null})
	 * @throws FileNotFoundException if the resource cannot be resolved as
	 * an absolute file path, i.e. is not available in a file system
	 * @throws IOException in case of general resolution/reading failures
	 */
	protected File getFileForLastModifiedCheck() throws IOException {
		return getFile();
	}

	/**
	 * 抛出 FileNotFoundException 异常，交给子类实现
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
	}

	/**
	 * 获取资源名称，默认返回 null ，交给子类实现
	 */
	@Override
	@Nullable
	public String getFilename() {
		return null;
	}


	/**
	 * This implementation compares description strings.
	 * @see #getDescription()
	 */
	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof Resource &&
				((Resource) other).getDescription().equals(getDescription())));
	}

	/**
	 * This implementation returns the description's hash code.
	 * @see #getDescription()
	 */
	@Override
	public int hashCode() {
		return getDescription().hashCode();
	}

	/**
	 * 返回资源文件的描述
	 */
	@Override
	public String toString() {
		return getDescription();
	}

}
