package codec;

import java.io.IOException;
import java.util.HashMap;

import io.BitSink;
import io.BitSource;
import io.InsufficientBitsLeftException;

public class LwzEncoder {

	private String size;
	private int max;
	HashMap<String, Integer> d = new HashMap<>();
	private boolean _closed;
	
	
	public LwzEncoder(HashMap dictionary, int m) {
		d = dictionary;
		size = Integer.toBinaryString(m);
		//make a bunch of 0s to the max length
		max = size.length();
	}
	
	public HashMap getDictionary(){
		return d;
	}
	
	public void close(BitSink out) throws IOException {
		if (!_closed) {
			_closed = true;
			out.write(0x80000000, 32);
			out.padToWord();
		}
	}
	
	
	//////////////////////////////////////////////////////////////////////
	//////////finds what needs to be written to the file//////////////////

	public void encode(int[][] frame, BitSink out) throws IOException {

		if (d == null) {
			throw new RuntimeException("No source hash installed");
		}
		
		if (_closed) {
			throw new RuntimeException("Lzw encoder already closed");
		}
		

			int width = frame.length;
			int height = frame[0].length;
			
			String workingKey = Integer.toString(frame[0][0]);
			String testKey = workingKey;
			int counter = 0;

			for (int y=0; y<height; y++) {
				for (int x=0; x<width; x++) {
					counter =x;
					workingKey =Integer.toString(frame[x][y]);
					testKey = workingKey;
					if(d.containsKey(workingKey)) {
						while(d.containsKey(testKey)) {			
							if(counter==width-1) {
								y+=1;
								x=0;
								counter = x;
							}else {
								counter++;
							}
							workingKey =testKey;
							if(y<height) {
								testKey += " " + frame[counter][y];
							} else {
								break;
							}
						}
						if(y == height) {
							x = width;
						} else {
							x = counter - 1;
						}
						//System.out.println("encoded " + workingKey + " to " + d.get(workingKey));

						writeFile(workingKey,out);

					}
			
				}
			}
	}

	
	////////////////////////////////////////////////////////////////////////////
	/////////writes specific value to the file using the string key/////////////
	
	public void writeFile(String s,BitSink out) throws IOException {

		if (_closed) {

			throw new RuntimeException("Attempt to encode symbol on closed encoder");

		}

		if (d.containsKey(s)) {
			String x = Integer.toBinaryString(d.get(s));
			int length = x.length();
			int diff = 0;
			String paddedWithzero;
			
			if(length < max){
				diff = max - length;
			//get the values to have 0s in front up to the max by oring it with the max 0s
				paddedWithzero = String.format("%0" + diff + "d%s", 0, x);
			}else {
				paddedWithzero = x;
			}
			
			out.write(paddedWithzero);
			
		} else {

			throw new RuntimeException("Symbol not in code map");
			
		}
	}
	
	
	
	

}
