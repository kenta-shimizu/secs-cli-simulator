package com.shimizukenta.secs.secs2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Secs2List extends AbstractSecs2 {
	
	private static final long serialVersionUID = -2380901931225887256L;
	
	private static final Secs2Item secs2Item = Secs2Item.LIST;
	
	private static final String SPACE = "  ";
	private static final String BR = System.lineSeparator();
	

	private final List<Secs2> values;
	private String proxyToString;
	private String proxyToJson;
	
	public Secs2List() {
		super();
		
		this.values = Collections.emptyList();
		this.proxyToString = null;
		this.proxyToJson = null;
	}

	public Secs2List(Secs2... values){
		super();
		
		this.values = Stream.of(values).collect(Collectors.toList());
		this.proxyToString = null;
		this.proxyToJson = null;
	}

	public Secs2List(List<? extends Secs2> values) {
		super();
		
		this.values = new ArrayList<>(values);
		this.proxyToString = null;
		this.proxyToJson = null;
	}

	@Override
	public int size() {
		return values.size();
	}
	
	@Override
	public Iterator<Secs2> iterator() {
		return values.iterator();
	}
	
	@Override
	public Stream<Secs2> stream() {
		return values.stream();
	}
	
	@Override
	protected void putBytesPack(Secs2BytesPackBuilder builder) throws Secs2BuildException {
		
		this.putHeaderBytesToBytesPack(builder, size());
		
		for ( Secs2 ss : values ) {
			
			if ( ss instanceof AbstractSecs2 ) {
				
				((AbstractSecs2)ss).putBytesPack(builder);
				
			} else {
				
				throw new Secs2BuildException("cast failed");
			}
		}
	}
	
	@Override
	protected AbstractSecs2 get( LinkedList<Integer> list ) throws Secs2Exception {
		
		if ( list.isEmpty() ) {
			
			return this;
			
		} else {
			
			try {
				int index = list.removeFirst();
				Secs2 ss = values.get(index);
				
				if ( ss instanceof AbstractSecs2 ) {
					
					return ((AbstractSecs2)ss).get(list);
					
				} else {
					
					throw new Secs2Exception("cast failed");
				}
			}
			catch ( IndexOutOfBoundsException e ) {
				throw new Secs2IndexOutOfBoundsException(e);
			}
		}
	}

	@Override
	public Secs2Item secs2Item() {
		return secs2Item;
	}
	
	@Override
	public String toString() {
		synchronized ( this ) {
			if ( this.proxyToString == null ) {
				this.proxyToString = lineStrings("").stream().collect(Collectors.joining(BR));
			}
			return this.proxyToString;
		}
	}
	
	private List<String> lineStrings(String space) {
		
		final List<String> lines = new ArrayList<>();
		
		lines.add(space + "<" + secs2Item().symbol() + " [" + size() + "]");
		
		values.stream()
		.map(s -> {
			
			String ss = space + SPACE;
			
			if ( s instanceof Secs2List ) {
				
				return ((Secs2List)s).lineStrings(ss).stream()
						.collect(Collectors.joining(BR));
				
			} else {
				
				return ss + s.toString();
			}
		})
		.forEach(lines::add);
		
		lines.add(space + ">");
		
		return lines;
	}
	
	@Override
	public String toJson() {
		synchronized ( this ) {
			if ( this.proxyToJson == null ) {
				this.proxyToJson = super.toJson();
			}
			return this.proxyToJson;
		}
	}
	
	@Override
	protected String toJsonValue() {
		
		return stream().map(ss -> ss.toJson())
				.collect(Collectors.joining(",", "[", "]"));
	}
	
	@Override
	protected String toStringValue() {
		return "";
	}

}
