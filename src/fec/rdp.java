package fec;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Erasure code RDP.Java version
 * @author Roger Song
 *
 */
public class rdp implements fec{
	
    private int disks;
    private int stripe_unit_size;
    private static final int TOLERENCE = 2; // RDP can protect data against 2 failures
    private static final int DATA_LENGTH = 1024; // default data length 
    private int pnumRdp;    // a prime number
    private int w;
    private int allDisks;  // disk num or data unit num
    private char[][] idata;
    private char[][] odata;
    private char[][] data;
    
    private BitSet inthis;

	public rdp(){
		this.disks = 4;
		this.pnumRdp = 257;
		this.stripe_unit_size = DATA_LENGTH;
		this.w = pnumRdp - 1;
		this.allDisks = disks + TOLERENCE;

		idata = new char[allDisks][stripe_unit_size];
		odata = new char[TOLERENCE][stripe_unit_size];
		data = new char[allDisks][stripe_unit_size];
		
		inthis = new BitSet();

	}
	
	public rdp(int disks, int pnumRdp, int dataLength ){
		this.pnumRdp = pnumRdp;
		this.stripe_unit_size = dataLength;
		this.w = pnumRdp - 1;
		this.disks = disks;
		this.allDisks = disks + TOLERENCE;
		
		idata = new char[allDisks][stripe_unit_size];
		odata = new char[TOLERENCE][stripe_unit_size];
		data = new char[allDisks][stripe_unit_size];
		
		inthis = new BitSet();

	}
	
    public void setErrData(int[] err)
    {
        for(int i = 0; i < err.length; i++ )
        {
        	if(1 == err[i]){
        		inthis.set(i);
        	}
        }
    }
	

	/**
	 *  an easy test case
	 */
	public void setData(){
		for(int i = 0; i < disks; i++){
			for(int j = 0; j < stripe_unit_size; j++){
				idata[i][j]=(char) ('a' + i);
			}
		}
		
		System.arraycopy(idata[0], 0, odata[0], 0, stripe_unit_size);
		System.arraycopy(idata[0], 0, odata[1], 0, stripe_unit_size);
	}
	
	public String showme(){
		return "RDP";
	}
	

	/**
	 * rdp encoding algorithm
	 */
	private void rdp_encoding(){
		
		int off, d, p, diag;
		int packet_size = stripe_unit_size /w;
		
		for (d = 1; d < disks; d++) {
			
			diag = d;

			for (p = 0; p < w; p++) {
				if (diag <= w - 1)
				{
					for (off = 0; off < packet_size; off++) {

						odata[0][p * packet_size + off]^= idata[d][p * packet_size + off];
						odata[1][diag * packet_size + off]^= idata[d][p * packet_size + off];
					}
					diag++;

				}
				else
				{
					
					for (off = 0; off < packet_size; off++) {

						odata[0][p * packet_size + off]^= idata[d][p * packet_size + off];
					}
					diag = 0;
					
				}
				
			}
		}
		for (p = 1; p < w;p++)
		{
			for (off = 0;off < packet_size;off++)
			{
				odata[1][(p - 1) * packet_size + off]^= odata[0][p * packet_size + off];

			}
		}

		System.arraycopy(odata[0], 0, idata[disks], 0, stripe_unit_size);
		System.arraycopy(odata[1], 0, idata[disks+1], 0, stripe_unit_size);

	}
	

	/**
	 * rdp encoding main function. 
	 * there is a simple testcase in setData func 
	 */
	public void encoding(){
		
//!		setData();
		outputRes();
		rdp_encoding();
	}
	

	/**
	 * rdp decoding main function.
	 * errorNum:[1,2]  one/two:the first/second error disk  rError: r checkout is broken?
	 * @param errorNum  
	 * @param one
	 * @param two
	 * @param rError
	 */
	public void decoding(){
		
		int errNUm = 0;
		int oneData = 0,one = 0;
		int twoData = 0,two = 0;
		boolean rError = false;
		int errCount = 0;
		
		for(int i = 0; i < allDisks; i++){
			if(inthis.get(i)){
				errCount++;
				if(1 == errCount){
					oneData = i + 2;
					one = i;
					if(one < disks){
						errNUm++;
					}
				}
				else if(2 == errNUm){
					twoData = i + 2;
					two = i;
					if(two < disks){
						errNUm++;
					}
				}
			}
		}
		
		if(disks == two){
			rError = true;
		}
		
		// rdp 
		if(0 == errNUm){
			System.out.println("No Error data need be recovery!");
			return;
		}
		if(errCount > 2){
			System.out.println("Error NUM is too larger! It should be [1,2]");
			return;
		}
		if(errNUm == 2 && (one < 0 || one >= disks || two < 0 || two >= disks)){
			System.out.println("Error NUM is 2, but detailed error col numbers are wrong! Thay are should be [0,disks)");
			return;
		}
		
		System.arraycopy(idata[disks], 0, data[0], 0, stripe_unit_size);
		System.arraycopy(idata[disks+1], 0, data[1], 0, stripe_unit_size);
		
		for(int i = 0; i < disks; i++){
			System.arraycopy(idata[i], 0, data[i+2], 0, stripe_unit_size);
		}	
		
		for(int i = 0; i < stripe_unit_size; i++){

			data[oneData][i] = 0;
		}
		
		if(errNUm == 1){
			if(rError){
				for(int i = 0; i < stripe_unit_size; i++){
					data[0][i] = 0;
				}
				
				rdp_decoding_d(data, disks, stripe_unit_size, w, oneData);
				System.arraycopy(data[oneData], 0, idata[one], 0, stripe_unit_size);
			}
			else{
				rdp_decoding_r(data, disks, stripe_unit_size, w, oneData);
				System.arraycopy(data[oneData], 0, idata[one], 0, stripe_unit_size);
			}
		}
		else{
			for(int i = 0; i < stripe_unit_size; i++){

				data[oneData][i] = 0;
				data[twoData][i] = 0;
			}
			rdp_decoding_rd(data, disks, stripe_unit_size, w, oneData, twoData);
			System.arraycopy(data[oneData], 0, idata[one], 0, stripe_unit_size);
			System.arraycopy(data[twoData], 0, idata[two], 0, stripe_unit_size);
		}
		
	}
	

	/**
	 * rdp decoding according r and d checkouts.
	 * x/y:  the first/second error disk in g_data
	 * @param g_data  
	 * @param disks
	 * @param stripe_unit_size
	 * @param w
	 * @param x
	 * @param y
	 */
	private void rdp_decoding_rd(char[][] g_data, int disks, int stripe_unit_size, int w,int x,int y)
	{
		int packet_size = stripe_unit_size / w;
		int ccount, count, rcount;
		char[] rdata, xdata, ydata, row_data, diag_data;
		int g, gx, gy,coffset,i,j,k,c;
		int row_disk, diag_disk;
		
		rcount=stripe_unit_size;
	
		rdata=g_data[0];
		xdata=g_data[x];
		ydata=g_data[y];
		gx = (x >= 3 ? x - 3 : pnumRdp - 1); 
		gy = (y >= 3 ? y - 3 : pnumRdp - 1);
		row_disk = x;
		diag_disk = y;
		row_data = xdata;
		diag_data = ydata;
		g=gx;
		
		while(true) {
			if(g == pnumRdp - 1) {
				if(gx == (row_disk >= 3 ? row_disk - 3 : row_disk + pnumRdp - 3)) {
					row_disk = y;
					diag_disk = x;
					row_data = ydata;
					diag_data = xdata; 
					g = gy;
					continue;
				}
				else
					break;
			}
			else {
				int  row_index;
				char[]  cdata;  
				int diag_count;
				int row_count;
				diag_count=stripe_unit_size;
				row_count=stripe_unit_size;
				row_index = (g - diag_disk + pnumRdp + 2) % pnumRdp;
				coffset = (row_index + diag_disk - 2 + pnumRdp) % pnumRdp * packet_size;
				cdata=g_data[1];
				for(i = row_index * packet_size, j = coffset, k = 0;i < diag_count &&k < packet_size; i++, j++, k++)
				{
					diag_data[i] = cdata[j];
				}
				for(c = 2; c < (disks+2); c++) {					
					if(c == diag_disk)
						continue;
					ccount=stripe_unit_size;

		            cdata = g_data[c];
					coffset = (row_index + diag_disk - c + pnumRdp) 
						% pnumRdp * packet_size;
					for(i = row_index * packet_size, j = coffset, k = 0;i < diag_count && j < ccount &&k < packet_size; i++, j++, k++)
					{

						diag_data[i] ^=  cdata[j];
					}		
				}

	            ccount = rcount;
	            cdata = rdata;
				coffset = (row_index + diag_disk - 1 + pnumRdp) 
				% pnumRdp * packet_size;

				for(i = row_index * packet_size, j = coffset, k = 0; i < diag_count && j < ccount &&k < packet_size; i++, j++, k++)
				{

					diag_data[i] ^=  cdata[j];
				}
				
				//row_parity calculate
				coffset = row_index * packet_size;
				for(c = 2; c < (disks+2); c++) {
					if(c == row_disk)
						continue;

					ccount=stripe_unit_size;

		            cdata = g_data[c];	
					coffset = row_index * packet_size;
					for(i = coffset, k = 0; i < ccount &&k < packet_size; i++, k++)
					{	

						row_data[i] ^= cdata[i];
					}  

				}
				cdata = rdata;
				coffset = row_index * packet_size;
				count = row_count;
				
				for(i = coffset, k = 0; i < count &&  k < packet_size; i++, k++)
				{
					row_data[i] ^= cdata[i];

				}		
				
				//calculate the next g
				g = (row_index + row_disk - 2) % pnumRdp;

			}
		}
		
	}

	
	/**
	 * rdp decoding according r and d checkouts.
	 * x:  the first error disk in g_data
	 * @param g_data  
	 * @param disks
	 * @param stripe_unit_size
	 * @param w
	 * @param x
	 */
	private void rdp_decoding_d(char[][] g_data,  int disks, int stripe_unit_size, int w, int x)
	{
		int xcount, ccount, count, dcount;
		int packet_size = stripe_unit_size / w;
		int gr,g,gx;
		int row_disk, diag_disk;
		char[] row_data, diag_data, rdata, xdata;
		int coffset;
		int i,j,k,c;
		int diag_count;
		int row_count;
		
		
		dcount=stripe_unit_size;
		xcount=stripe_unit_size;
		gr = pnumRdp - 2; 
		gx = (x >= 3 ? x - 3 : pnumRdp - 1);
		row_disk = pnumRdp + 1;
		diag_disk = x;
		rdata=g_data[0];
		xdata=g_data[x];
		row_data=rdata;
		diag_data=xdata;
		g = gr;
		diag_count = xcount;
		row_count = dcount;
		
		
		while(true) {
			if(g == pnumRdp - 1) {
				if(gr == (row_disk >= 3 ? row_disk - 3 : row_disk + pnumRdp -3)) {
					row_disk = x;
					diag_disk = pnumRdp + 1;
					row_data = xdata;
					diag_data =rdata;
					g = gx;
					row_count = diag_count;
					diag_count = dcount;
	
					continue;
				}
				else
					break;
			}
			else {
				int row_index;
				char[] cdata;  
				row_index = (g - diag_disk + pnumRdp + 2) % pnumRdp;
			       coffset = (row_index + diag_disk - 2 + pnumRdp) % pnumRdp * packet_size;
				cdata=g_data[1];
				for(i = row_index * packet_size, j = coffset, k = 0; i < diag_count && k < packet_size; i++, j++, k++)
				{
					diag_data[i] = cdata[j];
	
				}
				for(c = 2; c <(disks+2); c++) {
					if((int)c == diag_disk)
						continue;
				
					
					ccount=stripe_unit_size;
					cdata = g_data[c];
					coffset = (row_index + diag_disk - c + pnumRdp) 
						% pnumRdp * packet_size;
	
					for(i = row_index * packet_size, j = coffset, k = 0;i < diag_count && j < ccount && k < packet_size; i++, j++, k++)
					{
						diag_data[i] ^=  cdata[j];
	
					}
						
				}
				if(pnumRdp + 1 != diag_disk)
				{
					ccount=stripe_unit_size;
					cdata = g_data[0];
					coffset = (row_index + diag_disk - 1 + pnumRdp) 
						% pnumRdp * packet_size;
	
					for(i = row_index * packet_size, j = coffset, k = 0;i < diag_count && j < ccount && k < packet_size; i++, j++, k++)
					{
						diag_data[i] ^=  cdata[j];
	
					}
				}
				//row_parity calculate
				for(c = 2; c <(disks+2); c++) {
					if((int)c == row_disk)
						continue;
	
					ccount=stripe_unit_size;
					cdata = g_data[c];				
					coffset = row_index * packet_size;
	
					for(i = coffset, k = 0;i < ccount && k < packet_size; i++, k++)
					{	
						row_data[i] ^= cdata[i];

					}
	
				
				}
				if(row_disk != pnumRdp + 1) {
					
					//diag_disk must be DISK---VDEV_RAIDZ_R 
					count = row_count;
					cdata = diag_data;
					coffset = row_index * packet_size;
	
					for(i = coffset, k = 0; i < count && k < packet_size; i++, k++)
						row_data[i] ^= cdata[i];
	
				}
				//calculate the next g
				g = (row_index + row_disk - 2) % pnumRdp;	
				
			}
		}
	}
	

	/**
	 * rdp decoding according r and d checkouts.
	 * k:  the first error disk in g_data
	 * @param g_data  
	 * @param disks
	 * @param stripe_unit_size
	 * @param w
	 * @param k
	 */
	private void rdp_decoding_r(char[][] g_data, int disks, int stripe_unit_size, int w, int k)
	
	{
		int off, p;
		int packet_size = stripe_unit_size / w;
		int i=0;
	
		System.arraycopy(g_data[0], 0, g_data[k], 0, stripe_unit_size);
		for(i=2;i<(disks+2);i++)
		{
			if(i==k) continue;
			for (p = 0; p < w; p++)
			{	
				for (off = 0; off < packet_size; off++)
		 		{ 
		 		 	g_data[k][p * packet_size + off]^=g_data[i][p * packet_size + off];
				}
			}	
		}
		
	}

	/**
	 * for testing and debug.
	 */
	public void outputRes(){
		
		for(int i=0; i < disks; i++)
		{
			System.out.printf("idata:%d:  ",i);
			System.out.println(idata[i]);
		}
		System.out.print("odata:0:  ");
		System.out.println(odata[0]);
		System.out.print("odata:1:  ");
		System.out.println(odata[1]);
	}
	
	/**
	 * for testing and debug.
	 */
	public void outputData(){
		
		System.out.println("The res:");
		System.out.print("odata:0:  ");
		System.out.println(odata[0]);
		System.out.print("odata:1:  ");
		System.out.println(odata[1]);
	}

	/**
	 * for testing and debug.
	 */
	public void outputOrigin(){
		
		System.out.println("After decoding:");
		for(int i=0; i < disks; i++)
		{
			System.out.printf("idata:%d:  ",i);
//			System.out.println(idata[i]);
			System.out.println(Arrays.toString(idata[i]));
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final int NUM = 6;
		int[] err = new int[NUM];
		
		System.out.println("starting");
		rdp rdpItem = new rdp();

		rdpItem.encoding();
		rdpItem.outputData();
		
        for(int i=0;i<NUM;i++){
            err[i] = 0;
        }
        
//        err[0]=1;
 //       err[2]=1;
        err[5]=1;
        
        rdpItem.setErrData(err);
		
		// testing one error, error disk sequence number is 3, r checkout disk is not broken
//		rdpItem.decoding(1, 3, -1, false);
//		rdpItem.outputOrigin();
		
		// testing one error, error disk sequence number is 1, r checkout disk is  broken
//		rdpItem.decoding(1, 1, -1, true);
//		rdpItem.outputOrigin();
		
		// testing 2 error, the error disks sequence number is 0 and 3, r checkout disk is not broken
		rdpItem.decoding();
//		rdpItem.decoding(2, 0, 3, true);
		rdpItem.outputOrigin();
	}

}



