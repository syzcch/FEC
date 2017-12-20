package fec;

import java.util.*;


/**
 * Erasure code RDP.Java version
 * @author Roger Song
 *
 */
public class Rscode implements Fec{
//	private static final int prim_poly_32 = 020000007;
//	private static final int prim_poly_16 = 0210013;
	private static final int prim_poly_8 = 0435;
//	private static final int prim_poly_4 = 023;
//	private static final int prim_poly_2 = 07;
	
	static int[] B_TO_J;
	static int[] J_TO_B;
	static int j_to_b_idx;
	static int Modar_M;
	static int Modar_N;
	static int Modar_Iam;

	private int num; // original data cols num
	private char[][] rs; // original data
	private static final int FT_NUM = 2; //default checksum num
	private static int gf_already_setup;
	private int allNum;
	private static final int DATA_LENGTH = 1024; // default stripe size
	private int stripe_unit_size;  // stripe size
	private int rsNum;
 //   private int[] inthis;
    private BitSet inthis;
    
    public Rscode()
    {
        allNum = 6;
        rsNum = FT_NUM;
        num = allNum - rsNum;
        stripe_unit_size = DATA_LENGTH;
        gf_already_setup = 0;
        j_to_b_idx = 0;

        // use allnum here, rs includes original data and redudant data
        rs = new char[allNum][stripe_unit_size];  
        inthis = new BitSet();
    }
    
    public Rscode(int allnum, int rsnum, int dataLength)
    {
        allNum = allnum;
        rsNum = rsnum;
        num = allNum - rsNum;
        stripe_unit_size = dataLength;
        gf_already_setup = 0;
        j_to_b_idx = 0;
        
        // use allnum here, rs includes original data and redudant data
        rs = new char[allNum][stripe_unit_size];  
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
	public void setData()
	{
		for(int i = 0; i < num; i++){
			for(int j = 0; j < stripe_unit_size; j++){
				rs[i][j]=(char) ('a' + i);
			}
		}
	}
	
	public String showme(){
		return "RS";
	}
	
    /**
	 * for testing and debug.
	 */
	public void outputOrigin(){
		
		System.out.println("After decoding:");
		for(int i=0; i < num; i++)
		{
			System.out.printf("data:%d:  ",i);
			System.out.println(rs[i]);
		}
	}
	
    /**
	 * for testing and debug.
	 */
	public void outputData()
	{
		
		System.out.print("The res:");
        for(int i = num; i < allNum; i++)
        {
        	System.out.println(rs[i]);
        }
	}
	
    /**
     * Multiplies to elements of the field.
     */
    char multiply(char a, char b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        else {
            int logA = B_TO_J[a & 0xFF];
            int logB = B_TO_J[b & 0xFF];
            int logResult = logA + logB;
            return (char) J_TO_B[logResult];
        }
    }
	
    /**
	 * rs encoding main function. 
	 * there is a simple testcase in setData func 
	 */
	 public void encoding()
     {
		if(allNum != num + rsNum){
			throw new IllegalArgumentException("the addition of data disk num and checksum disk num should equal to allNum");
		}
        int cols,rows;
        int[] factors;
        int[] vdm;
        int z=rsNum,n=0;
	    char[][] buffer;

        n=num;
	    cols=n;
	    rows=z+n;
        factors = new int[n];
        buffer = new char[n][stripe_unit_size];

        for(int i=0;i<n;i++)
        {
     	   for(int l=0;l<stripe_unit_size;l++)
     	   {
    		   buffer[i][l] = rs[i][l];
     	   }
        }

        for (int i = 0; i < n; i++){ 
            factors[i] = 1;
        }

        vdm = gf_make_dispersal_matrix(rows, cols);
        
        for (int iByte = 0; iByte < stripe_unit_size; iByte++) {
            for (int iRow = num; iRow < allNum; iRow++) {

                int value = 0;
                for (int c = 0; c < num; c++) {
                    value ^= multiply((char)vdm[iRow * num + c], rs[c][iByte]);
                }
                rs[iRow][iByte] = (char) value;
            }
        }

     }
	 
	    public void decoding()
	    {
	        int[] vdm;
	        Condensed_Matrix cm;
	        int rows,cols;
	        int m,n;
	        int[] exists;
	        int[] factors;
	        int[] map;
	        char[][] buffer;
	        int[] id;
	        int[] mat;
	        int[] inv;
	        char[][] buff = null;

	        int err = 0;

	        buff = new char[allNum][stripe_unit_size];

	        m=rsNum;
	        n=num;
	        cols=n;
	        rows=m+n;
	        vdm = gf_make_dispersal_matrix(rows, cols);
	        exists = new int[rows];

	        factors = new int[rows];

	        map = new int[rows];
	        buffer = new char[allNum][stripe_unit_size];
	        
	        for(int j = 0; j < (m+n); j++){
	        	for(int i = 0; i<stripe_unit_size; i++)
	        	{
	        	    buff[j][i] = rs[j][i];
	        	} 
	        }

	        err = 0;
	        for (int i = 0; i <rows && err <cols; i++) 
	  	    {
	  	        if (inthis.get(i)){
	               map[i]=-1;
	  	        }
		        else{
	               map[i] = err++;
	               for(int l=0;l<stripe_unit_size;l++){ 
	                   buffer[map[i]][l]=buff[i][l]; 
	               }
	            }
	  	    }
	        
	        err = 0;
	        for (int i = 0; i < cols; i++){
	            if (map[i] == -1){ 
	            	err++;  //map == -1 means data loss or data corruption
	            }
	        }

	        System.out.printf("Blocks to decode: %d\n", err);

	        for (int i = 0; i < rows; i++){
	        	if(map[i] != -1){
	        		exists[i] = 1;
	        	}
//	            exists[i] = (map[i] != -1);
	        }
	        cm = gf_condense_dispersal_matrix(vdm, exists, rows, cols);
	        mat = cm.condensed_matrix;  
	        id = cm.row_identities;  
	        for (int i = 0; i < cols; i++) {
	            if (map[i] == -1){ 
	                map[i] = map[id[i]];
	            }
	        }

	        inv = gf_invert_matrix(mat, cols);

	        for(int i = 0; i < rows; i++){
	            factors[i] = 1;
	        }
	        
	        for (int iByte = 0; iByte < stripe_unit_size; iByte++) {
	            for (int iRow = 0; iRow < cols; iRow++) {
	                if (id[iRow] >= cols){

	                    int value = 0;
	                    for (int c = 0; c < num; c++) {
	                        value ^=multiply((char) inv[iRow * num + c], buffer[map[c]][iByte]);
	                    }
	                    rs[iRow][iByte] = (char) value;
	                }
	            }
	        }
	    }
	    
	    public String code_show(){

	        return "rs";
	    }
	    
	    void gf_modar_setup()
	    {
	        int j, b;
	        int res;
	        if (1 == gf_already_setup) return;
	        B_TO_J = new int[256];

	        if (B_TO_J == null) {
	        	throw new RuntimeException("BUG: gf_modar_setup, malloc B_TO_J");
	        }
	        /* When the word size is 8 bits, make three copies of the table so that
	          you don't have to do the extra addition or subtraction in the
	          multiplication/division routines */

	        J_TO_B = new int[256*3];
	        if (J_TO_B == null) {
	        	throw new RuntimeException("BUG: gf_modar_setup, malloc J_TO_B");
	        }
	        for (j = 0; j < 256; j++) {
	            B_TO_J[j] = 255;
	            J_TO_B[j_to_b_idx + j] = 0;
	        } 

	        b = 1;
	        for (j = 0; j < 255; j++) {
	            if (B_TO_J[b] != 255) {
	            	throw new RuntimeException("Error: j=%d, b=%d, B->J[b]=%d, J->B[j]=%d (0%o)");
	            }
	            B_TO_J[b] = j;
	            J_TO_B[j_to_b_idx + j] = b;
	            b = b << 1;
	            res = b & 256;
	            if(0 != res){
//	            if (b & 256){
	            
	                b = (b ^ prim_poly_8) & 255;
	            }
	        }

	        for (j = 0; j < 255; j++) {
	            J_TO_B[j_to_b_idx + j + 255] = J_TO_B[j_to_b_idx + j];
	            J_TO_B[j_to_b_idx + j + 2*255] = J_TO_B[j_to_b_idx + j];
	        }
	        j_to_b_idx = j_to_b_idx + 255;


	        gf_already_setup = 1;

	    }
	    
	    int gf_single_multiply(int xxx, int yyy)
	    {
	        int sum_j;
	        int zzz;

	        gf_modar_setup();
	        if (xxx == 0 || yyy == 0) {
	            zzz = 0;
	        } else {
	            sum_j = (int) (B_TO_J[xxx] + (int) B_TO_J[yyy]);
	            zzz = J_TO_B[j_to_b_idx + sum_j];
	        }
	        return zzz;
	    }
	    
	    int gf_single_divide(int a, int b)
	    {
	        int sum_j;
	        gf_modar_setup();
	        if (b == 0) 
	            return -1;
	        if (a == 0) 
	            return 0;
	        sum_j = B_TO_J[a] - B_TO_J[b];

	        return (int) J_TO_B[j_to_b_idx + sum_j];
	    }
	    
	    int gf_log(int value)
	    {
	       return B_TO_J[value];
	    }
	    
	    /* This returns the rows*cols vandermonde matrix.  N+M must be
	       < 2^w -1.  Row 0 is in elements 0 to cols-1.  Row one is 
	       in elements cols to 2cols-1.  Etc.*/
	    
	    int[] gf_make_vandermonde(int rows, int cols)
	    {
	        int[]vdm;
	        int i, j, k;
	        gf_modar_setup();

	        if (rows >= 255 || cols >= 255) {
	        	throw new RuntimeException("Error: gf_make_vandermonde: %d + %d >= %d");
	        }
	     
	        vdm = new int[rows * cols];

	        for (i = 0; i < rows; i++) {
	            k = 1;
	            for (j = 0; j < cols; j++) {
	                vdm[i*cols+j] = k;
	                k = gf_single_multiply(k, i);
	            }
	        }
	        return vdm;
	    }
	    
	    static int find_swap_row(int[] matrix, int rows, int cols, int row_num)
	    {
	        int j;

	        for (j = row_num; j < rows; j++) {
	            if (matrix[j*cols+row_num] != 0) 
	            	return j;
	        }
	        return -1;
	    }
	    
	    int[] gf_make_dispersal_matrix(int rows, int cols)
	    {
	        int[] vdm;
	        int i, j, k, l, inv, tmp, colindex;

	        vdm = gf_make_vandermonde(rows, cols);


	        for (i = 0; i < cols && i < rows; i++) {
	            j = find_swap_row(vdm, rows, cols, i);
	            if (-1 == j) {
	            	throw new RuntimeException("Error: make_dispersal_matrix.  Can't find swap row %d");
	            }

	            if (j != i) {
	                for (k = 0; k < cols; k++) {  
	                    tmp = vdm[j*cols+k];
	                    vdm[j*cols+k] = vdm[i*cols+k];
	                    vdm[i*cols+k] = tmp;
	                }
	            }
	            if (vdm[i*cols+i] == 0) {
	            	throw new RuntimeException("Internal error -- this shouldn't happen");
	            }

	            if (vdm[i*cols+i] != 1) {
	                inv = gf_single_divide(1, vdm[i*cols+i]);
	                k = i;
	                for (j = 0; j < rows; j++) {
	                    vdm[k] = gf_single_multiply(inv, vdm[k]);
	                    k += cols;
	                }

	            }
	            if (vdm[i*cols+i] != 1) {
	            	throw new RuntimeException("Internal error -- this shouldn't happen #2)");
	            }

	            for (j = 0; j < cols; j++) {
	                colindex = vdm[i*cols+j];
	                if (j != i && colindex != 0) {
	                    k = j;
	                    for (l = 0; l < rows; l++) {
	                        vdm[k] = vdm[k] ^ gf_single_multiply(colindex, vdm[l*cols+i]);
	                        k += cols;
	                    }
	                }
	            }
	        }

	        return vdm;
	    }
	    

	    Condensed_Matrix gf_condense_dispersal_matrix(
	                        int[] disp, int[] existing_rows, int rows,    int  cols)
	    {
	        Condensed_Matrix cm;
	        int[] m;
	        int[] id;
	        int i, j, k, tmp;

	        /* Allocate cm and initialize */
	        cm = new Condensed_Matrix(); 

	        cm.condensed_matrix = new int[cols*cols];

	        cm.row_identities = new int[cols];

	        m = cm.condensed_matrix;
	        id = cm.row_identities;
	        for (i = 0; i < cols; i++){
	            id[i] = -1;
	        }

	        /* First put identity rows in their proper places */
	        for (i = 0; i < cols; i++) {
	            if (existing_rows[i] != 0) {
	                id[i] = i;
	                tmp = cols*i;
	                for (j = 0; j < cols; j++){
	                    m[tmp+j] = disp[tmp+j];
	                }
	            }
	        }

	      /* Next, put coding rows in */
	      k = 0;
	      for (i = cols; i < rows; i++) {
	        if (existing_rows[i] != 0) {
	            while(k < cols && id[k] != -1){ 
	                k++;
	            }
	            if (k == cols){ 
	                return cm;
	            }
	            id[k] = i;
	            for (j = 0; j < cols; j++)
	            {
	                m[cols*k+j] = disp[cols*i+j];
	            }
	        }
	      }

	      /* If we're here, there are no more coding rows -- check to see that the
	         condensed dispersal matrix is full -- otherwise, it's not -- return an
	         error */

	      while(k < cols && id[k] != -1){
	          k++;
	      }
	      if (k == cols){ 
	          return cm;
	      }

	      return null;
	    }
	    
	    int[] gf_invert_matrix(int[] mat, int rows)
	    {
	        int[] inv;
	        int[] copy;
	        int cols, i, j, k, x, rs2;
	        int row_start, tmp, inverse;
	     
	        cols = rows;

	        inv = new int[rows*cols];

	        copy = new int[rows*cols];
	        k = 0;
	        for (i = 0; i < rows; i++) {
	            for (j = 0; j < cols; j++) {
	                inv[k] = (i == j) ? 1 : 0;
	                copy[k] = mat[k];
	                k++;
	            }
	        }

	        /* First -- convert into upper triangular */
	        for (i = 0; i < cols; i++) {
	            row_start = cols*i;

	            /* Swap rows if we ave a zero i,i element.  If we can't swap, then the 
	            matrix was not invertible */

	            if (copy[row_start+i] == 0) { 
	                for (j = i+1; j < rows && copy[cols*j+i] == 0; j++) ;
	                if (j == rows) {
	                	throw new RuntimeException("gf_invert_matrix: Matrix not invertible!!");
	                }
	                rs2 = j*cols;
	                for (k = 0; k < cols; k++) {
	                    tmp = copy[row_start+k];
	                    copy[row_start+k] = copy[rs2+k];
	                    copy[rs2+k] = tmp;
	                    tmp = inv[row_start+k];
	                    inv[row_start+k] = inv[rs2+k];
	                    inv[rs2+k] = tmp;
	                }
	            }
	     
	            /* Multiply the row by 1/element i,i */
	            tmp = copy[row_start+i];
	            if (tmp != 1) {
	                inverse = gf_single_divide(1, tmp);
	                for (j = 0; j < cols; j++) { 
	                    copy[row_start+j] = gf_single_multiply(copy[row_start+j], inverse);
	                    inv[row_start+j] = gf_single_multiply(inv[row_start+j], inverse);
	                }
	                /* pic(inv, copy, rows, "Divided through"); */
	            }

	            /* Now for each j>i, add A_ji*Ai to Aj */
	            k = row_start+i;
	            for (j = i+1; j != cols; j++) {
	                k += cols;
	                if (copy[k] != 0) {
	                    if (copy[k] == 1) {
	                        rs2 = cols*j;
	                        for (x = 0; x < cols; x++) {
	                            copy[rs2+x] ^= copy[row_start+x];
	                            inv[rs2+x] ^= inv[row_start+x];
	                        }
	                    } 
	                    else {
	                        tmp = copy[k];
	                        rs2 = cols*j;
	                        for (x = 0; x < cols; x++) {
	                            copy[rs2+x] ^= gf_single_multiply(tmp, copy[row_start+x]);
	                            inv[rs2+x] ^= gf_single_multiply(tmp, inv[row_start+x]);
	                        }
	                    }   
	                }
	            }
	            /* pic(inv, copy, rows, "Eliminated rows"); */
	        }

	        /* Now the matrix is upper triangular.  Start at the top and multiply down */
	        for (i = rows-1; i >= 0; i--) {
	            row_start = i*cols;
	            for (j = 0; j < i; j++) {
	                rs2 = j*cols;
	                if (0 != copy[rs2+i]) {
	                    tmp = copy[rs2+i];
	                    copy[rs2+i] = 0; 
	                for (k = 0; k < cols; k++) {
	                    inv[rs2+k] ^= gf_single_multiply(tmp, inv[row_start+k]);
	                }
	            }
	        }
	        /* pic(inv, copy, rows, "One Column"); */
	      }

	      return inv;
	    }

	    int[] gf_matrix_multiply(int[] a, int[] b, int cols)
	    {
	        int[] prod;
	        int i, j, k;

	        prod = new int[cols*cols];

	        for (i = 0; i < cols*cols; i++){
	            prod[i] = 0;
	        }

	        for (i = 0; i < cols; i++) {
	            for (j = 0; j < cols; j++) {
	                for (k = 0; k < cols; k++) {
	                    prod[i*cols+j] ^= gf_single_multiply(a[i*cols+k], b[k*cols+j]);
	                }
	            }
	        }
	        return prod;
	    }
	    
		/**
		 * @param args
		 */
		public static void main(String[] args) {
		    final int NUM = 10;
		    System.out.println("starting");
//			rscode *rsItem = new rscode();

		    //all data is 10, checksum data is 3, data stripe length is 1024
		    Rscode rsItem = new Rscode(10,3,1024);
//		    Rscode rsItem = new Rscode();
		    int[] err = new int[NUM];

		    // 0 means fault data
		    for(int i=0;i<NUM;i++){
		        err[i] = 0;
		    }

		    rsItem.setData();
			rsItem.encoding();
			rsItem.outputData();
			
			
			// testing 3 errors, error disk sequence number is 0,1,3
			err[0]=1;
		    err[1]=1;
		    err[3]=1;
		    rsItem.setErrData(err);
		    rsItem.decoding();
			rsItem.outputOrigin();
		}

}
