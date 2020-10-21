package com.shimizukenta.secssimulator.extendsml;

import com.shimizukenta.secs.sml.SmlDataItemParser;
import com.shimizukenta.secs.sml.SmlParseException;

public class ExtendSmlDataItemParser extends SmlDataItemParser {

	protected ExtendSmlDataItemParser() {
		super();
	}
	
	private static class SingletonHolder {
		private static ExtendSmlDataItemParser inst = new ExtendSmlDataItemParser();
	}
	
	public static ExtendSmlDataItemParser getInstance() {
		return SingletonHolder.inst;
	}
	
	private static final String itemNow = "NOW";
	
	private static final String itemU4Auto = "U4AUTO";
	private static final String itemU8Auto = "U8AUTO";
	private static final String itemI4Auto = "I4AUTO";
	private static final String itemI8Auto = "I8AUTO";
	
	@Override
	protected SeekValueResult parseExtend(String str, int fromIndex, String secs2ItemString, int size)
			throws SmlParseException {
		
		if ( secs2ItemString.equals(itemNow) ) {
			return parseNow(str, fromIndex, size);
		}
		
		//HOOK
		//others
		
		throw new SmlParseException("UNKNOWN SECS2ITEM type: " + secs2ItemString);
	}
	
	private SeekValueResult parseNow(String str, int fromIndex, int size)
		throws SmlParseException {
		
		SeekCharResult r = this.seekAngleBranketEnd(str, fromIndex);
		return seekValueResult(Secs2Now.now(size), r.index + 1);
	}
	
}
