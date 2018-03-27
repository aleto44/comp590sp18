package codec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.BitSink;
import io.BitSource;
import io.InsufficientBitsLeftException;
import models.Symbol;

public class LwzDecoder{

	HashMap<String, Integer> d = new HashMap<>();
	HashMap<Integer, String> invD = new HashMap<>();
	private int max;
	private String size;
	private int curIndex = 0;
	
	public LwzDecoder(HashMap dictionary,HashMap inverseDic, int m){
		d = dictionary;
		invD = inverseDic;
		size = Integer.toBinaryString(m);
		//make a bunch of 0s to the max length
		max = size.length();
	}
	
	
	///////////////////////////////////////////////
	///gets values stored in the file and finds strings from the values
	///parses the strings and puts all this stuff in a grand big array
	///////////////////////////////////////////////
	
	public int[] decode(LwzDecoder decoder, BitSource bit_source, int width, int height) throws IOException, InsufficientBitsLeftException {
		int pixStrings[] = new int[width*height];
		curIndex = 0;
		
		while(curIndex < width*height){
			int x = bit_source.next(max);
			String[] tokens = invD.get(x).split(" ");
			for(int b = 0; b < tokens.length; b++){
				pixStrings[curIndex] = Integer.valueOf(tokens[b]);
				curIndex++;
				}
		System.out.println("Current pixel decoding: " + curIndex);
		}
		return pixStrings;
		}
	}


