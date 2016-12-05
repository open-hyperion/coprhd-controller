/*
 * MIT License
 *
 * Copyright (c) 2016 Lavar Askew (open.hyperion@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package open.hyperion.purestorage.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CompleteError {
    private String _errorResp;
    private int    _httpCode;
    private String _pureStorageCode;
    
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public String getErrorResp() {
        return _errorResp;
    }
 
    public void setErrorResp(String errorResp) {
        _errorResp = errorResp;
    }
 
    public int getHttpCode() {
        return httpCode;
    }
 
    public void setHttpCode(int httpCode) {
        _httpCode = httpCode;
    }
 
    public String getHp3parCode() {
        return _pureStorageCode;
    }
 
    public void setHp3parCode(String pureStorageCode) {
        _pureStorageCode = pureStorageCode;
    }
}