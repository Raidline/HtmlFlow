/*
 * MIT License
 *
 * Copyright (c) 2014-18, mcarvalho (gamboa.pt) and lcduarte (github.com/lcduarte)
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

package htmlflow.visitor;

import org.xmlet.htmlapifaster.Element;
import org.xmlet.htmlapifaster.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the base implementation of the ElementVisitor (from HtmlApiFaster
 * library) which collects information about visited Html elements of a HtmlView.
 * The HTML static content is collected into an internal cached cacheBlocksList.
 * Static content is interleaved with dynamic content.
 *
 * @author Miguel Gamboa, Luís Duare
 *         created on 17-01-2018
 */
public abstract class HtmlVisitorCache extends HtmlVisitor {
    /**
     * The begin index of a static HTML block.
     */
    private int staticBlockIndex = 0;
    /**
     * Signals the begin of a dynamic partial view and thus it should stop
     * collecting the HTML into the cache.
     */
    private boolean openDynamic = false;
    /**
     * True when the first visit is finished and all static blocks of HTML
     * are cached in cacheBlocksList.
     */
    private boolean isCached = false;
    /**
     * A cache list of static html blocks.
     */
    private final List<HtmlBlockInfo> cacheBlocksList = new ArrayList<>();
    /**
     * The current index in cacheBlocksList corresponding to a static HTML block.
     */
    private int cacheIndex = 0;

    HtmlVisitorCache(boolean isIndented) {
        super(isIndented);
    }

    /**
     * This visitor may be writing to output or not, depending on the kind of HTML
     * block that it is being visited.
     * So, it should just write to output immediately only when it is:
     *   1. in a static block that is not already in cache,
     * or
     *   2 in a dynamic block that is never cached and thus must be always freshly
     *   written to the output.
     */
    public final boolean isWriting() {
        return !isCached || openDynamic;
    }
    /**
     * While the static blocks are not in cache then it appends elements to
     * the main StringBuilder or PrintStream.
     * Once already cached then it does nothing.
     * This method appends the String {@code "<elementName"} and it leaves the element
     * open to include additional attributes.
     * Before that it may close the parent begin tag with {@code ">"} if it is
     * still opened (!isClosed).
     * The newlineAndIndent() is responsible for this job to check whether the parent element
     * is still opened or not.
     *
     * @param element
     */
    @Override
    public final void visitElement(Element element) {
        if (isWriting()){
            super.visitElement(element);
        }
    }

    /**
     * Writes the end tag for elementName: {@code "</elementName>."}.
     * This visit occurs when the º() is invoked.
     */
    @Override
    public final void visitParent(Element element) {
        if (isWriting()){
            super.visitParent(element);
        }
    }

    @Override
    public final void visitAttribute(String attributeName, String attributeValue) {
        if (isWriting()){
            super.visitAttribute(attributeName, attributeValue);
        }
    }

    @Override
    public final <R> void visitText(Text<? extends Element, R> text) {
        if (isWriting()){
            super.visitText(text);
        }
    }


    @Override
    public final <R> void visitComment(Text<? extends Element, R> text) {
        if (isWriting()){
            super.visitComment(text);
        }
    }

    /**
     * Copies from or to cacheBlocksList depending on whether the content is in cache or not.
     * Copying from cacheBlocksList will write through write() method.
     * Copying to cacheBlocksList will read from substring().
     */
    @Override
    public final void visitOpenDynamic(){
        if (openDynamic )
            throw new IllegalStateException("You are already in a dynamic block! Do not use dynamic() chained inside another dynamic!");

        openDynamic = true;
        if (isCached){
            HtmlBlockInfo block = cacheBlocksList.get(cacheIndex);
            this.write(block.html);
            this.depth = block.currentDepth;
            this.isClosed = block.isClosed;
            ++cacheIndex;
        } else {
            String staticBlock = substring(staticBlockIndex);
            cacheBlocksList.add(new HtmlBlockInfo(staticBlock, depth, isClosed));
        }
    }

    @Override
    public final void visitCloseDynamic(){
        openDynamic = false;
        if (!isCached){
            staticBlockIndex = size();
        }
    }

    public final String finished(){
        if (isCached && cacheIndex <= cacheBlocksList.size()){
            HtmlBlockInfo block = cacheBlocksList.get(cacheIndex);
            write(block.html);
            isClosed = block.isClosed;
            depth = block.currentDepth;
        }

        if (!isCached){
            String staticBlock = substring(staticBlockIndex);
            cacheBlocksList.add(new HtmlBlockInfo(staticBlock, depth, isClosed));
            isCached = true;
        }

        String result = readAndReset();
        cacheIndex = 0;
        return result;
    }

    static class HtmlBlockInfo {

        final String html;
        final int currentDepth;
        final boolean isClosed;

        HtmlBlockInfo(String html, int currentDepth, boolean isClosed){
            this.html = html;
            this.currentDepth = currentDepth;
            this.isClosed = isClosed;
        }
    }

    /**
     * Returns a substring with the HTML content from the index staticBlockIndex
     */
    protected abstract String substring(int staticBlockIndex);

    /**
     * Returns the accumulated output and clear it.
     */
    protected abstract String readAndReset();
}
