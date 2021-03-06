/*
 * Copyright (c) 2004 Nu Echo Inc.
 * 
 * This is free software. For terms and warranty disclaimer, see ./COPYING
 */
package org.schemeway.plugins.schemescript.editor;

import org.eclipse.jface.text.*;
import org.schemeway.plugins.schemescript.parser.*;

public class SchemeDoubleClickStrategy implements ITextDoubleClickStrategy {
    private SexpNavigator mExpressionNavigator;
    private ITextViewer mText;

    public void doubleClicked(ITextViewer part) {
        mText = part;
        createExplorer(part);

        int pos = part.getSelectedRange().x;

        if (pos < 0)
            return;

        if (! (selectComment(pos) || selectString(pos) || selectSpaces(pos))) {
        	selectSExpression(pos);
        }
    }

    private void createExplorer(ITextViewer part) {
        if (mExpressionNavigator == null || mExpressionNavigator.getDocument() != part.getDocument()) {
            if (mExpressionNavigator != null)
                mExpressionNavigator.dispose();
            mExpressionNavigator = new SexpNavigator(part.getDocument());
        }
    }

    protected boolean selectComment(int caretPos) {
        ITypedRegion partition = SchemeTextUtilities.getPartition(mExpressionNavigator.getDocument(), caretPos);
		if (partition.getType() == SchemePartitionScanner.SCHEME_COMMENT) {
		    selectRange(partition.getOffset(), partition.getOffset() + partition.getLength());
		    return true;
		}
        return false;
    }

    protected boolean selectString(int caretPos) {
        ITypedRegion partition = SchemeTextUtilities.getPartition(mExpressionNavigator.getDocument(), caretPos);
		if (SchemePartitionScanner.isStringPartition(partition.getType())) {
		    selectRange(partition.getOffset(), partition.getOffset() + partition.getLength());
		    return true;
		}
        return false;
    }

    protected boolean selectSpaces(int caretPos) {
        try {
            IDocument document = mExpressionNavigator.getDocument();
            int length = document.getLength();
            if (caretPos > 0 && caretPos < length) {
                char chAfter = document.getChar(caretPos);
                char chBefore = document.getChar(caretPos - 1);
                if (SchemeScannerUtilities.isWhitespaceChar(chBefore)
                    && SchemeScannerUtilities.isWhitespaceChar(chAfter)) {
                    int startPos = caretPos - 1;
                    char ch = document.getChar(startPos);
                    while (startPos >= 0 && SchemeScannerUtilities.isWhitespaceChar(ch)) {
                        startPos--;
                        ch = document.getChar(startPos);
                    }
                    int endPos = caretPos + 1;
                    ch = document.getChar(endPos);
                    while (endPos < length && SchemeScannerUtilities.isWhitespaceChar(ch)) {
                        endPos++;
                        ch = document.getChar(endPos);
                    }
                    selectRange(startPos + 1, endPos);
                    return true;
                }
            }
        }
        catch (BadLocationException x) {
        }
        return false;
    }

    protected boolean selectSExpression(int caretPos) {
        try {
            IDocument document = mExpressionNavigator.getDocument();
            char ch = document.getChar(caretPos);
            if (ch == '(') {
                if (mExpressionNavigator.forwardSexpression(caretPos)) {
                    selectRange(mExpressionNavigator.getSexpStart(), mExpressionNavigator.getSexpEnd());
                    return true;
                }
                else
                    return false;
            }
            else
                if (ch != ')' && !SchemeScannerUtilities.isWhitespaceChar(ch)) {
                    if (mExpressionNavigator.forwardSexpression(caretPos)) {
                        mExpressionNavigator.backwardSexpression(mExpressionNavigator.getSexpEnd());
                        selectRange(mExpressionNavigator.getSexpStart(), mExpressionNavigator.getSexpEnd());
                        return true;
                    }
                    else
                        return false;
                }

            if (caretPos > 0)
                ch = document.getChar(caretPos - 1);
            else
                return false;

            if (ch == ')') {
                if (mExpressionNavigator.backwardSexpression(caretPos)) {
                    selectRange(mExpressionNavigator.getSexpStart(), mExpressionNavigator.getSexpEnd());
                    return true;
                }
            }
        }
        catch (BadLocationException exception) {
        }
        return false;
    }

    private void selectRange(int startPos, int stopPos) {
        mText.setSelectedRange(startPos, stopPos - startPos);
    }
}