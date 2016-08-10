package fec;

import java.util.*;

/**
 * Erasure code RDP.Java version. Using ArrayList instead of Array.
 * @author Roger Song
 *
 */
public class rdpArrayList {
	
    private int disks;
    private int stripe_unit_size;
    private static final int TOLERENCE = 2; // RDP can protect data against 2 failures
    private static final int DATA_LENGTH = 1024; // default data length 
    private int pnumRdp;    // a prime number
    private int w;
    private int allDisks;  // disk num or data unit num
    private List<ArrayList<Character>> idata;
    private List<ArrayList<Character>> odata;
    private List<ArrayList<Character>> data;

	public rdpArrayList(){
		this.disks = 4;
		this.pnumRdp = 257;
		this.stripe_unit_size = DATA_LENGTH;
		this.w = pnumRdp - 1;
		this.allDisks = disks + TOLERENCE;

		idata = new ArrayList<ArrayList<Character>>(disks);
		for(int i = 0; i < disks; i++){
			ArrayList<Character> tmpList = new ArrayList<Character>(stripe_unit_size);
			idata.add(tmpList);
		}
		
		odata = new ArrayList<ArrayList<Character>>(TOLERENCE);
		for(int i = 0; i < TOLERENCE; i++){
			ArrayList<Character> tmpList = new ArrayList<Character>(stripe_unit_size);
			odata.add(tmpList);
		}
		
		data = new ArrayList<ArrayList<Character>>(allDisks);
		for(int i = 0; i < allDisks; i++){
			ArrayList<Character> tmpList = new ArrayList<Character>(stripe_unit_size);
			data.add(tmpList);
		}
	}
	
	public rdpArrayList(int disks, int pnumRdp, int dataLength ){
		this.pnumRdp = pnumRdp;
		this.stripe_unit_size = dataLength;
		this.w = pnumRdp - 1;
		this.disks = disks;
		this.allDisks = disks + TOLERENCE;
		
		idata = new ArrayList<ArrayList<Character>>(disks);
		for(int i = 0; i < disks; i++){
			ArrayList<Character> tmpList = new ArrayList<Character>(stripe_unit_size);
			idata.add(tmpList);
		}
		
		odata = new ArrayList<ArrayList<Character>>(TOLERENCE);
		for(int i = 0; i < TOLERENCE; i++){
			ArrayList<Character> tmpList = new ArrayList<Character>(stripe_unit_size);
			odata.add(tmpList);
		}
		
		data = new ArrayList<ArrayList<Character>>(allDisks);
		for(int i = 0; i < allDisks; i++){
			ArrayList<Character> tmpList = new ArrayList<Character>(stripe_unit_size);
			data.add(tmpList);
		}
	}
	

	/**
	 *  an easy test case
	 */
	public void setData(){
		for(int i = 0; i < disks; i++){
			for(int j = 0; j < stripe_unit_size; j++){
				ArrayList<Character> tmpArrayList = idata.get(i);
				tmpArrayList.add((char) ('a' + i));
			}
		}
		
		odata.get(0).addAll(idata.get(0));
		odata.get(1).addAll(idata.get(0));
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
						odata.get(0).set(p * packet_size + off, (char)(odata.get(0).get(p * packet_size + off)^idata.get(d).get(p * packet_size + off)));
						odata.get(1).set(diag * packet_size + off, (char)(odata.get(1).get(diag * packet_size + off) ^ idata.get(d).get(p * packet_size + off)));
					}
					diag++;

				}
				else
				{
					
					for (off = 0; off < packet_size; off++) {

						odata.get(0).set(p * packet_size + off, (char)(odata.get(0).get(p * packet_size + off) ^ idata.get(d).get(p * packet_size + off)));
					}
					diag = 0;
					
				}
				
			}
		}
		for (p = 1; p < w;p++)
		{
			for (off = 0;off < packet_size;off++)
			{
				odata.get(1).set((p-1) * packet_size + off, (char)(odata.get(1).get((p-1) * packet_size + off) ^ odata.get(0).get(p * packet_size + off)));

			}
		}


	}
	

	/**
	 * rdp encoding main function. 
	 * there is a simple testcase in setData func 
	 */
	public void encoding(){
		
		setData();
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
	public void decoding(int errorNum, int one, int two, Boolean rError){
		
		int oneData = one + 2;
		int twoData = two + 2;
		// rdp 
		if(errorNum > 2 || errorNum < 1){
			System.out.println("Error NUM is too larger or smaller! It should be [1,2]");
			return;
		}
		if(errorNum == 2 && (one < 0 || one >= disks || two < 0 || one >= disks)){
			System.out.println("Error NUM is 2, but detailed error col numbers are wrong! Thay are should be [0,disks)");
			return;
		}
		
//		System.arraycopy(odata[0], 0, data[0], 0, stripe_unit_size);
//		System.arraycopy(odata[1], 0, data[1], 0, stripe_unit_size);
//		Collections.copy(data.get(0), odata.get(0));
		data.get(0).addAll(odata.get(0));
//		Collections.copy(data.get(1), odata.get(1));
		data.get(1).addAll(odata.get(1));
		
		for(int i = 0; i < disks; i++){
//			Collections.copy(data.get(i+2), idata.get(i));
			data.get(i+2).addAll(idata.get(i));
//			System.arraycopy(idata[i], 0, data[i+2], 0, stripe_unit_size);
		}	
		
		for(int i = 0; i < stripe_unit_size; i++){

			data.get(oneData).set(i,(char)0);
		}
		
		if(errorNum == 1){
			if(rError){
				for(int i = 0; i < stripe_unit_size; i++){
					data.get(0).set(i,(char)0);
				}
				
				rdp_decoding_d(data, disks, stripe_unit_size, w, oneData);
//				System.arraycopy(data[oneData], 0, idata[one], 0, stripe_unit_size);
				Collections.copy(idata.get(one), data.get(oneData));
			}
			else{
				rdp_decoding_r(data, disks, stripe_unit_size, w, oneData);
//				System.arraycopy(data[oneData], 0, idata[one], 0, stripe_unit_size);
				Collections.copy(idata.get(one), data.get(oneData));
			}
		}
		else{
			for(int i = 0; i < stripe_unit_size; i++){

				data.get(oneData).set(i,(char)0);
				data.get(twoData).set(i,(char)0);
			}
			rdp_decoding_rd(data, disks, stripe_unit_size, w, oneData, twoData);
			Collections.copy(idata.get(one), data.get(oneData));
			Collections.copy(idata.get(two), data.get(twoData));
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
	private void rdp_decoding_rd(List<ArrayList<Character>> g_data, int disks, int stripe_unit_size, int w,int x,int y)
	{
		int packet_size = stripe_unit_size / w;
		int ccount, count, rcount;
		ArrayList<Character> rdata, xdata, ydata, row_data, diag_data;
		int g, gx, gy,coffset,i,j,k,c;
		int row_disk, diag_disk;
		
		rcount=stripe_unit_size;
	
		rdata=g_data.get(0);
		xdata=g_data.get(x);
		ydata=g_data.get(y);
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
				ArrayList<Character>  cdata;  
				int diag_count;
				int row_count;
				diag_count=stripe_unit_size;
				row_count=stripe_unit_size;
				row_index = (g - diag_disk + pnumRdp + 2) % pnumRdp;
				coffset = (row_index + diag_disk - 2 + pnumRdp) % pnumRdp * packet_size;
				cdata=g_data.get(1);
				for(i = row_index * packet_size, j = coffset, k = 0;i < diag_count &&k < packet_size; i++, j++, k++)
				{
					diag_data.set(i, cdata.get(j));
				}
				for(c = 2; c < (disks+2); c++) {					
					if(c == diag_disk)
						continue;
					ccount=stripe_unit_size;

					cdata = g_data.get(c);
					coffset = (row_index + diag_disk - c + pnumRdp) 
						% pnumRdp * packet_size;
					for(i = row_index * packet_size, j = coffset, k = 0;i < diag_count && j < ccount &&k < packet_size; i++, j++, k++)
					{
						diag_data.set(i, (char)(diag_data.get(i) ^ cdata.get(j)));
					}		
				}

	            ccount = rcount;
	            cdata = rdata;
				coffset = (row_index + diag_disk - 1 + pnumRdp) 
				% pnumRdp * packet_size;

				for(i = row_index * packet_size, j = coffset, k = 0; i < diag_count && j < ccount &&k < packet_size; i++, j++, k++)
				{

					diag_data.set(i, (char)(diag_data.get(i) ^ cdata.get(j)));
				}
				
				//row_parity calculate
				coffset = row_index * packet_size;
				for(c = 2; c < (disks+2); c++) {
					if(c == row_disk)
						continue;

					ccount=stripe_unit_size;

					cdata = g_data.get(c);
					coffset = row_index * packet_size;
					for(i = coffset, k = 0; i < ccount &&k < packet_size; i++, k++)
					{	
						row_data.set(i, (char)(row_data.get(i) ^ cdata.get(i)));
					}  

				}
				cdata = rdata;
				coffset = row_index * packet_size;
				count = row_count;
				
				for(i = coffset, k = 0; i < count &&  k < packet_size; i++, k++)
				{
					row_data.set(i, (char)(row_data.get(i) ^ cdata.get(i)));

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
	private void rdp_decoding_d(List<ArrayList<Character>> g_data,  int disks, int stripe_unit_size, int w, int x)
	{
		int xcount, ccount, count, dcount;
		int packet_size = stripe_unit_size / w;
		int gr,g,gx;
		int row_disk, diag_disk;
		ArrayList<Character> row_data, diag_data, rdata, xdata;
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

		rdata=g_data.get(0);
		xdata=g_data.get(x);
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
				ArrayList<Character> cdata;  
				row_index = (g - diag_disk + pnumRdp + 2) % pnumRdp;
			    coffset = (row_index + diag_disk - 2 + pnumRdp) % pnumRdp * packet_size;

				cdata=g_data.get(1);
				for(i = row_index * packet_size, j = coffset, k = 0; i < diag_count && k < packet_size; i++, j++, k++)
				{
					diag_data.set(i, (char)(diag_data.get(i) ^ cdata.get(j)));
				}
				for(c = 2; c <(disks+2); c++) {
					if((int)c == diag_disk)
						continue;		
					
					ccount=stripe_unit_size;
					cdata = g_data.get(c);
					coffset = (row_index + diag_disk - c + pnumRdp) 
						% pnumRdp * packet_size;
	
					for(i = row_index * packet_size, j = coffset, k = 0;i < diag_count && j < ccount && k < packet_size; i++, j++, k++)
					{
						diag_data.set(i, (char)(diag_data.get(i) ^ cdata.get(j)));
					}
						
				}
				if(pnumRdp + 1 != diag_disk)
				{
					ccount=stripe_unit_size;
					cdata = g_data.get(0);
					coffset = (row_index + diag_disk - 1 + pnumRdp) 
						% pnumRdp * packet_size;
	
					for(i = row_index * packet_size, j = coffset, k = 0;i < diag_count && j < ccount && k < packet_size; i++, j++, k++)
					{
						diag_data.set(i, (char)(diag_data.get(i) ^ cdata.get(j)));
					}
				}
				//row_parity calculate
				for(c = 2; c <(disks+2); c++) {
					if((int)c == row_disk)
						continue;
	
					ccount=stripe_unit_size;	
					cdata = g_data.get(c);
					coffset = row_index * packet_size;
	
					for(i = coffset, k = 0;i < ccount && k < packet_size; i++, k++)
					{	
						row_data.set(i, (char)(row_data.get(i) ^ cdata.get(i)));
					}
	
				
				}
				if(row_disk != pnumRdp + 1) {
					
					//diag_disk must be DISK---VDEV_RAIDZ_R 
					count = row_count;
					cdata = diag_data;
					coffset = row_index * packet_size;
	
					for(i = coffset, k = 0; i < count && k < packet_size; i++, k++){
						row_data.set(i, (char)(row_data.get(i) ^ cdata.get(i)));
					}
	
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
	private void rdp_decoding_r(List<ArrayList<Character>> g_data, int disks, int stripe_unit_size, int w, int k)
	
	{
		int off, p;
		int packet_size = stripe_unit_size / w;
		int i=0;
	
		Collections.copy(g_data.get(k), g_data.get(0));
		
		for(i=2;i<(disks+2);i++)
		{
			if(i==k) continue;
			for (p = 0; p < w; p++)
			{	
				for (off = 0; off < packet_size; off++)
		 		{ 
					g_data.get(k).set(p * packet_size + off, (char)(g_data.get(k).get(p * packet_size + off) ^ g_data.get(i).get(p * packet_size + off)));
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
			System.out.println(idata.get(i));
		}
		System.out.print("odata:0:  ");
		System.out.println(odata.get(0));
		System.out.print("odata:1:  ");
		System.out.println(odata.get(1));
	}
	
	/**
	 * for testing and debug.
	 */
	public void outputOdata(){
		
		System.out.println("The res:");
		System.out.print("odata:0:  ");


		System.out.println(odata.get(0));
		System.out.print("odata:1:  ");
		System.out.println(odata.get(1));
	}

	/**
	 * for testing and debug.
	 */
	public void outputOrigin(){
		
		System.out.println("After decoding:");
		for(int i=0; i < disks; i++)
		{
			System.out.printf("idata:%d:  ",i);
			System.out.println(Arrays.toString(idata.get(i).toArray()));
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		System.out.println("starting");
		rdp rdpItem = new rdp();

		rdpItem.encoding();
		rdpItem.outputOdata();
		
		// testing one error, error disk sequence number is 3, r checkout disk is not broken
//		rdpItem.decoding(1, 2, -1, false);
//		rdpItem.outputOrigin();
		
		// testing one error, error disk sequence number is 1, r checkout disk is  broken
//		rdpItem.decoding(1, 0, -1, true);
//		rdpItem.outputOrigin();
		
		// testing 2 error, the error disks sequence number is 0 and 3, r checkout disk is not broken
		rdpItem.decoding(2, 2, 3, true);
		rdpItem.outputOrigin();
		
	}

}



