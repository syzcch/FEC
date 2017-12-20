package fectest;
import fec.*;


public class Fectest {
	
	public static void main(String[] args) {
		final int NUM = 7;
		int[] err = new int[NUM];
		
		Fec fecCode = new Rscode(10,3,1024);
//		fec fecCode = new starbyte();

		fecCode.setData();
		fecCode.encoding();
		
        for(int i=0;i<NUM;i++){
            err[i] = 0;
        }
        err[6]=1;
	    err[1]=1;
	    err[3]=1;
        
        fecCode.setErrData(err);
        fecCode.decoding();
        fecCode.outputOrigin();
	}
}
