/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.record.formula;

import java.util.Arrays;

import org.apache.poi.hssf.HSSFTestDataSamples;
import org.apache.poi.hssf.record.TestcaseRecordInputStream;
import org.apache.poi.hssf.record.UnicodeString;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
/**
 * Tests for <tt>ArrayPtg</tt>
 * 
 * @author Josh Micich
 */
public final class TestArrayPtg extends TestCase {

	private static final byte[] ENCODED_PTG_DATA = {
		0x40, 0x00,
		0x08, 0x00,
		0, 0, 0, 0, 0, 0, 0, 0, 
	};
	private static final byte[] ENCODED_CONSTANT_DATA = {
		2,    // 3 columns
		1, 0, // 2 rows
		4, 1, 0, 0, 0, 0, 0, 0, 0, // TRUE
		2, 4, 0, 0, 65, 66, 67, 68, // "ABCD"
		2, 1, 0, 0, 69, // "E"
		1, 0, 0, 0, 0, 0, 0, 0, 0, // 0
		4, 0, 0, 0, 0, 0, 0, 0, 0, // FALSE
		2, 2, 0, 0, 70, 71, // "FG"
	};

	/**
	 * Lots of problems with ArrayPtg's encoding of 
	 */
	public void testReadWriteTokenValueBytes() {
		
		ArrayPtg ptg = new ArrayPtgV(new TestcaseRecordInputStream(ArrayPtgV.sid, ENCODED_PTG_DATA));
		
		ptg.readTokenValues(new TestcaseRecordInputStream(0, ENCODED_CONSTANT_DATA));
		assertEquals(3, ptg.getColumnCount());
		assertEquals(2, ptg.getRowCount());
		Object[] values = ptg.token_3_arrayValues;
		assertEquals(6, values.length);
		
		
		assertEquals(Boolean.TRUE, values[0]);
		assertEquals(new UnicodeString("ABCD"), values[1]);
		assertEquals(new Double(0), values[3]);
		assertEquals(Boolean.FALSE, values[4]);
		assertEquals(new UnicodeString("FG"), values[5]);
		
		byte[] outBuf = new byte[ENCODED_CONSTANT_DATA.length];
		ptg.writeTokenValueBytes(outBuf, 0);
		
		if(outBuf[0] == 4) {
			throw new AssertionFailedError("Identified bug 42564b");
		}
		assertTrue(Arrays.equals(ENCODED_CONSTANT_DATA, outBuf));
	}

	/**
	 * Excel stores array elements column by column.  This test makes sure POI does the same.
	 */
	public void testElementOrdering() {
		ArrayPtg ptg = new ArrayPtgV(new TestcaseRecordInputStream(ArrayPtgV.sid, ENCODED_PTG_DATA));
		ptg.readTokenValues(new TestcaseRecordInputStream(0, ENCODED_CONSTANT_DATA));
		assertEquals(3, ptg.getColumnCount());
		assertEquals(2, ptg.getRowCount());
		
		assertEquals(0, ptg.getValueIndex(0, 0));
		assertEquals(2, ptg.getValueIndex(1, 0));
		assertEquals(4, ptg.getValueIndex(2, 0));
		assertEquals(1, ptg.getValueIndex(0, 1));
		assertEquals(3, ptg.getValueIndex(1, 1));
		assertEquals(5, ptg.getValueIndex(2, 1));
	}
	
	/**
	 * Test for a bug which was temporarily introduced by the fix for bug 42564.
	 * A spreadsheet was added to make the ordering clearer.
	 */
	public void testElementOrderingInSpreadsheet() {
		HSSFWorkbook wb = HSSFTestDataSamples.openSampleWorkbook("ex42564-elementOrder.xls");

		// The formula has an array with 3 rows and 5 column 
		String formula = wb.getSheetAt(0).getRow(0).getCell((short)0).getCellFormula();
		// TODO - These number literals should not have '.0'. Excel has different number rendering rules

		if (formula.equals("SUM({1.0,6.0,11.0;2.0,7.0,12.0;3.0,8.0,13.0;4.0,9.0,14.0;5.0,10.0,15.0})")) {
			throw new AssertionFailedError("Identified bug 42564 b");
		}
		assertEquals("SUM({1.0,2.0,3.0;4.0,5.0,6.0;7.0,8.0,9.0;10.0,11.0,12.0;13.0,14.0,15.0})", formula);
	}
}
