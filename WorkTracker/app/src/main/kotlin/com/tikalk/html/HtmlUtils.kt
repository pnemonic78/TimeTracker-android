/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * • Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * • Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * • Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.tikalk.html

import org.jsoup.internal.StringUtil
import org.jsoup.nodes.*
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

fun FormElement.selectByName(name: String): Element? {
    for (element in elements()) {
        if (element.attr("name") == name) {
            return element
        }
    }
    return null
}

fun FormElement.selectById(id: String): Element? {
    for (element in elements()) {
        if (element.attr("id") == id) {
            return element
        }
    }
    return null
}

fun Element.value(): String {
    return `val`()
}

fun Element.textBr(): String {
    val accum = StringUtil.borrowBuilder()
    NodeTraversor.traverse(object : NodeVisitor {
        override fun head(node: Node, depth: Int) {
            if (node is TextNode) {
                appendNormalisedText(accum, node)
            } else if (node is Element) {
                if (node.tagName() == "br") {
                    accum.append('\n')
                } else if (accum.isNotEmpty() && node.isBlock) {
                    accum.append(' ')
                }
            }
        }

        override fun tail(node: Node, depth: Int) { // make sure there is a space between block tags and immediately following text nodes <div>One</div>Two should be "One Two".
            if (node is Element) {
                if (node.isBlock && (node.nextSibling() is TextNode) && !lastCharIsWhitespace(accum)) {
                    accum.append(' ')
                }
            }
        }
    }, this)

    return StringUtil.releaseBuilder(accum).trim()
}

private fun appendNormalisedText(accum: StringBuilder, textNode: TextNode) {
    val text = textNode.wholeText
    if (textNode is CDataNode) {
        accum.append(text)
    } else {
        StringUtil.appendNormalisedWhitespace(accum, text, lastCharIsWhitespace(accum))
    }
}

private fun lastCharIsWhitespace(sb: StringBuilder): Boolean {
    return sb.isNotEmpty() && (sb[sb.lastIndex] == ' ')
}

fun findParentElement(element: Element, parentTag: String): Element? {
    var parent = element.parent()
    while (parent != null) {
        if (parent.tagName() == parentTag) {
            return parent
        }
        parent = parent.parent()
    }
    return null
}