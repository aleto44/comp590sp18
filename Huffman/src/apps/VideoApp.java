package apps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import models.Unsigned8BitModel;
import codec.HuffmanEncoder;
import codec.LwzDecoder;
import codec.LwzEncoder;
import codec.SymbolDecoder;
import codec.SymbolEncoder;
import models.Symbol;
import models.SymbolModel;
import models.Unsigned8BitModel.Unsigned8BitSymbol;
import io.InsufficientBitsLeftException;
import io.BitSink;
import io.BitSource;
import codec.ArithmeticDecoder;
import codec.ArithmeticEncoder;
import codec.HuffmanDecoder;
import io.InputStreamBitSource;
import io.OutputStreamBitSink;

public class VideoApp {
	
	public static int dictSize = 256;

	public static void main(String[] args) throws IOException, InsufficientBitsLeftException {
		String base = "bunny";
		String filename="/Users/alexj_000/tmp/" + base + ".450p.yuv";
		File file = new File(filename);
		int width = 800;
		int height = 450;
		int num_frames = 150;
		
		
		HashMap<String, Integer> dictionary = new HashMap<>();
		HashMap<Integer, String> inverseDic = new HashMap<>();
		
		for(int i = 0; i < 256; i++){	//
			String str = i+"";			//Initialized dictionary with all initial possible options
			dictionary.put(str, i);		//
			inverseDic.put(i, str);		//
		}								//
		///////////////////////////////////

		InputStream training_values = new FileInputStream(file);
		int[][] current_frame = new int[width][height];

		for (int f=0; f < num_frames; f++) {
			System.out.println("Adding difference to dictionary for frame" + f);
			int[][] prior_frame = current_frame;
			current_frame = readFrame(training_values, width, height);
			int[][] diff_frame = frameDifference(prior_frame, current_frame);
			
			addtoDictionary(dictionary, inverseDic, diff_frame);		//create dictionary here
		}
		System.out.println("size of HashMap " + dictionary.size());
		int max = findMaxBit(dictionary); //get the max bit that will show up in values
		training_values.close();		

		
		
		
		LwzEncoder encoder = new LwzEncoder(dictionary, max);  //encoder object

		
		InputStream message = new FileInputStream(file);

		File out_file = new File("/Users/alexj_000/tmp/" + base + "-compressed.dat");
		OutputStream out_stream = new FileOutputStream(out_file);
		BitSink bit_sink = new OutputStreamBitSink(out_stream);

		current_frame = new int[width][height];

		for (int f=0; f < num_frames; f++) {
			System.out.println("Encoding frame difference " + f);
			int[][] prior_frame = current_frame;
			current_frame = readFrame(message, width, height);

			int[][] diff_frame = frameDifference(prior_frame, current_frame);
			encodeFrameDifference(diff_frame, encoder, bit_sink);	//encode this frame
		}

		message.close();
		encoder.close(bit_sink);
		out_stream.close();

		BitSource bit_source = new InputStreamBitSource(new FileInputStream(out_file));
		OutputStream decoded_file = new FileOutputStream(new File("/Users/alexj_000/tmp/" + base + "-decoded.dat"));

		LwzDecoder decoder = new LwzDecoder(dictionary, inverseDic, max);

		current_frame = new int[width][height];

		for (int f=0; f<num_frames; f++) {
			System.out.println("Decoding frame " + f);
			int[][] prior_frame = current_frame;
			int[][] diff_frame = decodeFrame(decoder, bit_source, width, height);	//decode this frame
			current_frame = reconstructFrame(prior_frame, diff_frame);
			outputFrame(current_frame, decoded_file);
		}

		decoded_file.close();

	}


	///////////////////////////////////////////////////////////////////////////////////
	////////////////////This method will create the dictionary by looking at a frame///
	
	private static void addtoDictionary(HashMap dictionary, HashMap inverseDic, int[][] frame) {
		int width = frame.length;
		int height = frame[0].length;
		
		String str = "";
		String test;
		
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				int i = frame[x][y];
				String pix = "" + i;
				
				if(str.equals("")){
					test = pix;
				} else {
					test = str + " " + pix;
				}
				
				
				if(dictionary.containsKey(test)){
					str = test;
				} else {
					dictionary.put(test, dictSize++);
					inverseDic.put(dictSize - 1, test);
					str = pix;
				}
				
				
			}
		}
	}

	////////////////////////////////////////////////////////////////////////
	////////////Calls encode on ecoder object//////////////////////////////
	
	private static void encodeFrameDifference(int[][] frame, LwzEncoder encoder, BitSink bit_sink) 
			throws IOException {
	
		
		encoder.encode(frame, bit_sink);
	}
	
	
	
	////////////////////////////////////////////////////////////////////////
	////////////////////reads compressed file to get back diff frame////////

	private static int[][] decodeFrame(LwzDecoder decoder, BitSource bit_source, int width, int height) 
			throws InsufficientBitsLeftException, IOException {
		
		int[][] frame = new int[width][height];
		int [] pixelParts = new int[width*height];
		

		pixelParts = decoder.decode(decoder,  bit_source,  width,  height);
		int i = 0;
		for (int x = 0; x<width;x++){
			for(int y = 0; y < height; y++){
				frame[x][y] = pixelParts[i];
				i++;
			}
		}
		
		//TODO make 360000 into 850 by whatever array and return;
		
		return frame;
	}
	
	
	
	/////finds size of hashmap//////
	
	public static int findMaxBit(HashMap d){
		return d.size();
	}
	
	
	
	//////////given code///////////////////////
	///////////////////////////////////////////
	
	private static int[][] reconstructFrame(int[][] prior_frame, int[][] frame_difference) {
		int width = prior_frame.length;
		int height = prior_frame[0].length;

		int[][] frame = new int[width][height];
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				frame[x][y] = (prior_frame[x][y] + frame_difference[x][y])%256;
			}
		}
		return frame;
	}

	private static void outputFrame(int[][] frame, OutputStream out) 
			throws IOException {
		int width = frame.length;
		int height = frame[0].length;
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				out.write(frame[x][y]);
			}
		}
	}
	
	private static int[][] readFrame(InputStream src, int width, int height) 
			throws IOException {
		int[][] frame_data = new int[width][height];
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				frame_data[x][y] = src.read();
			}
		}
		return frame_data;
	}

	private static int[][] frameDifference(int[][] prior_frame, int[][] current_frame) {
		int width = prior_frame.length;
		int height = prior_frame[0].length;

		int[][] difference_frame = new int[width][height];

		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				difference_frame[x][y] = ((current_frame[x][y] - prior_frame[x][y])+256)%256;
			}
		}
		return difference_frame;
	}

	
} 
