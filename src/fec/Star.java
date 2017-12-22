package fec;

import static java.lang.System.out;

import java.util.Arrays;

public class Star implements Fec {

	private int block_size;
	private int p;
	private int block_nbr;/* block_nbr = p - 1 */
	private char[][] check_data;
	private int[] restarts;
	private int data_disk_nbr;
	private int allNum;
	private int stripe_unit_size; // stripe size
	private static final int TOLERENCE = 3; // STAR can protect data against 2
											// failures
	private static final int DATA_LENGTH = 1024; // default data length
	private static final int PRIME = 257; // a prime number

	public Star() {
		p = PRIME;
		data_disk_nbr = 4;
		stripe_unit_size = DATA_LENGTH; // 1024

		block_nbr = p - 1; // 256
		block_size = stripe_unit_size / block_nbr; // 1
		allNum = data_disk_nbr + TOLERENCE; // 7

		check_data = new char[allNum][stripe_unit_size];
		restarts = new int[allNum];
		for (int i = 0; i < allNum; i++) {
			restarts[i] = 0;
		}
	}

	public Star(int disk, int prime, int dataLength) {
		p = prime;
		data_disk_nbr = disk;
		stripe_unit_size = dataLength;

		block_nbr = p - 1;
		block_size = stripe_unit_size / block_nbr;
		allNum = data_disk_nbr + TOLERENCE;

		check_data = new char[allNum][stripe_unit_size];

		restarts = new int[allNum];
		for (int i = 0; i < allNum; i++) {
			restarts[i] = 0;
		}
	}

	/**
	 * an easy test case
	 */
	public void setData() {
		for (int i = 0; i < data_disk_nbr; i++) {
			for (int j = 0; j < stripe_unit_size; j++) {
				check_data[i][j] = (char) ('a' + i);
			}
		}
	}

	/**
	 * for testing and debug. 1 means error, default value is 0
	 */
	public void setErrData(int[] err) {
		for (int i = 0; i < allNum; i++) {
			restarts[i] = err[i];
		}
	}

	public String showme() {
		return "STAR";
	}

	/**
	 * for testing and debug.
	 */
	public void outputData() {

		out.println("The res:");
		for (int i = 0; i < allNum; i++) {
			out.println(check_data[i]);
		}
	}

	/**
	 * for testing and debug.
	 */
	public void outputOrigin() {

		out.println("After decoding:");
		for (int i = 0; i < data_disk_nbr; i++) {
			out.printf("data:%d:  ", i);
			out.println(check_data[i]);
		}
	}

	/**
	 * entry function for encoding
	 */
	public void encoding() {

		if (stripe_unit_size % block_nbr != 0) {
			throw new RuntimeException(" Cannot  striping. wrong DATA_LENGTH!");
		}

		STAR_encoding_row();
		STAR_encoding_diag1();
		STAR_encoding_diag2();
	}

	/**
	 * entry function for encoding computing checksum in every
	 * row,*check_data[p]
	 */
	private void STAR_encoding_row() {
		int i, j;
		for (i = 0; i < stripe_unit_size; i++) {
			for (j = 0; j < data_disk_nbr; j++) {
				check_data[data_disk_nbr][i] ^= check_data[j][i];
			}

		}
	}

	/**
	 * entry function for encoding computing checksum in every
	 * row,*check_data[p]
	 */
	void STAR_encoding_diag1() {
		int i, j, stripe, k;
		char[][] tmp;

		tmp = new char[block_nbr + 1][block_size];

		for (stripe = 0; stripe < block_nbr + 1; stripe++) {
			for (i = 0; i < data_disk_nbr; i++) {
				for (j = 0; j < block_size; j++) {
					k = (stripe - i + p) % p;
					if (k < block_nbr) {
						tmp[stripe][j] ^= check_data[i][(stripe - i + p) % p
								* block_size + j];

					}
				}

			}

		}

		/* after store diagonal line checksum in tmp[block_nbr] */
		/* we need using diagonal line checksum and s to do xor compute */
		for (i = 0; i < block_nbr; i++) {
			for (j = 0; j < block_size; j++) {
				tmp[i][j] = (char) (tmp[i][j] ^ tmp[block_nbr][j]);

			}
		}

		for (i = 0; i < block_nbr; i++) {
			System.arraycopy(tmp[i], 0, check_data[data_disk_nbr + 1], i
					* block_size, block_size);
		}
	}

	/**
	 * entry function for encoding diagonal line checksum, slope
	 * -1,*check_data[p+2]
	 */
	void STAR_encoding_diag2() {
		int i, j, stripe, k;
		char[] tmp;

		tmp = new char[p * block_size];

		for (stripe = 0; stripe < block_nbr + 1; stripe++) {
			for (i = 0; i < data_disk_nbr; i++) {
				for (j = 0; j < block_size; j++) {
					k = (stripe + i + p) % p;
					if (k < block_nbr)
						tmp[stripe * block_size + j] ^= check_data[i][k
								* block_size + j];
				}
			}
		}
		for (i = 0; i < block_nbr; i++) {
			for (j = 0; j < block_size; j++) {
				tmp[i * block_size + j] ^= tmp[block_nbr * block_size + j];
			}
		}

		System.arraycopy(tmp, 0, check_data[data_disk_nbr + 2], 0,
				stripe_unit_size);

	}

	/**
	 * main function for encoding
	 */
	public void decoding() {
		int i, j, k, m, stripe;
		int rs_nbr = 0; /* rs_nbr means the number of error */
		int rs_data_nbr = 0; /*
							 * rs_data_nbr means the number of error in original
							 * data
							 */
		int rs_check_nbr = 0;/*
							 * rs_check_nbr means the number of error in
							 * checksum data
							 */
		int rs_disk1 = -1;
		int rs_disk2 = -1;
		int rs_disk3 = -1;

		if (stripe_unit_size % block_nbr != 0) {
			throw new RuntimeException(" Cannot  striping. wrong DATA_LENGTH!");
		}

		for (i = 0; i < data_disk_nbr + 2; i++) {
			if (restarts[i] == 1) {
				rs_disk1 = i;
				break;
			}
		}
		if (rs_disk1 != -1) {
			for (i = rs_disk1 + 1; i < data_disk_nbr + 2; i++) {
				if (restarts[i] == 1) {
					rs_disk2 = i;
					break;
				}
			}
		}
		if (rs_disk2 != -1) {
			for (i = rs_disk2 + 1; i < data_disk_nbr + 2; i++) {
				if (restarts[i] == 1) {
					rs_disk3 = i;
					break;
				}
			}
		}

		if (rs_disk1 != -1) {
			Arrays.fill(check_data[rs_disk1], (char) 0);
		}
		if (rs_disk2 != -1) {
			Arrays.fill(check_data[rs_disk2], (char) 0);
		}
		if (rs_disk3 != -1) {
			Arrays.fill(check_data[rs_disk3], (char) 0);
		}

		// out.printf("rs_disks : %d %d %d\n", rs_disk1,rs_disk2,rs_disk3);

		for (i = 0; i <= data_disk_nbr + 2; i++)
			rs_nbr += restarts[i];

		if (TOLERENCE < rs_nbr) {
			throw new RuntimeException(" Too many error data!");
		}

		for (i = 0; i < data_disk_nbr; i++)
			rs_data_nbr += restarts[i];

		rs_check_nbr = rs_nbr - rs_data_nbr;

		if (rs_data_nbr == 0) {
			if (restarts[data_disk_nbr] == 1)
				STAR_encoding_row();
			if (restarts[data_disk_nbr + 1] == 1)
				STAR_encoding_diag1();
			if (restarts[data_disk_nbr + 2] == 1)
				STAR_encoding_diag2();
		}

		if (rs_data_nbr == 1) {
			if (rs_check_nbr <= 1) {
				Evenodd_decoding(restarts);
				if (restarts[data_disk_nbr + 2] == 1)
					STAR_encoding_diag2();
			}
			if (rs_check_nbr == 2) {
				if (restarts[data_disk_nbr] == 0)/* row checksum is correct */
				{
					for (i = 0; i < stripe_unit_size; i++) {
						for (j = 0; j <= data_disk_nbr; j++) {
							if (j != rs_disk1)
								check_data[rs_disk1][i] ^= check_data[j][i];
						}
					}
					STAR_encoding_diag1();
					STAR_encoding_diag2();
				}
				if (restarts[data_disk_nbr] == 1)/* row checksum is error */
				{
					if (restarts[data_disk_nbr + 2] == 1) {
						Evenodd_decoding(restarts);
						STAR_encoding_diag2();
					}
					if (restarts[data_disk_nbr + 1] == 1) {
						Evenodd_decoding_1(rs_disk1, rs_disk2);
						STAR_encoding_diag1();
					}
				}
			}
		}

		if (rs_data_nbr == 2) {
			if (rs_check_nbr == 0) {
				Evenodd_decoding(restarts);
			} else {
				/* rs_check_nbr == 1 */
				if (restarts[data_disk_nbr] == 1) {
					char[] tmp_for_s1s2;
					tmp_for_s1s2 = new char[block_size];

					for (i = 0; i < block_nbr; i++)/* s1 xor s2 */
					{
						for (j = 0; j < block_size; j++) {
							tmp_for_s1s2[j] ^= check_data[data_disk_nbr + 1][i
									* block_size + j];
							tmp_for_s1s2[j] ^= check_data[data_disk_nbr + 2][i
									* block_size + j];
						}
					}

					/* store s~~ */
					char[][] tmp;
					tmp = new char[3][p * block_size];

					for (i = 0; i < stripe_unit_size; i++) {
						for (j = 0; j <= data_disk_nbr; j++) {
							tmp[0][i] ^= check_data[j][i];
						}
					}
					for (stripe = 0; stripe < block_nbr + 1; stripe++) {
						for (i = 0; i < data_disk_nbr; i++) {
							for (j = 0; j < block_size; j++) {
								k = (stripe - i + p) % p;
								if (k < block_nbr) {
									tmp[1][stripe * block_size + j] ^= check_data[i][k
											* block_size + j];
								}
							}
						}
					}

					for (i = 0; i < stripe_unit_size; i++) {
						tmp[1][i] ^= check_data[data_disk_nbr + 1][i];
					}

					for (stripe = 0; stripe < block_nbr + 1; stripe++) {
						for (i = 0; i < data_disk_nbr; i++) {
							for (j = 0; j < block_size; j++) {
								k = (stripe + i + p) % p;
								if (k < block_nbr) {
									tmp[2][stripe * block_size + j] ^= check_data[i][k
											* block_size + j];
								}
							}
						}
					}

					for (i = 0; i < stripe_unit_size; i++) {
						tmp[2][i] ^= check_data[data_disk_nbr + 2][i];
					}
					/* Now,restore s~~ and put them in tmp */

					char[][] tmp_for_xor;
					tmp_for_xor = new char[p][block_size];

					for (i = 0; i < block_nbr + 1; i++) {
						for (j = 0; j < block_size; j++) {
							tmp_for_xor[i][j] = (char) (tmp[0][i * block_size
									+ j]
									^ tmp[0][((rs_disk2 - rs_disk1 + i) % p)
											* block_size + j]
									^ tmp[1][((rs_disk2 + p + i) % p)
											* block_size + j]
									^ tmp[2][((p - rs_disk1 + i) % p)
											* block_size + j] ^ tmp_for_s1s2[j]);
						}
					}

					/*
					 * *tmp[0] = c[p][0] xor c[s][rs_disk2 - rs_disk1]tmp[i] =
					 * c[p][i] xor c[s][(rs_disk2 - rs_disk1 + i)%p]
					 * ...................................
					 */
					k = p - 1 - (rs_disk2 - rs_disk1);/*
													 * c[p][k] xor c[p][p-1] =
													 * tmp_for_xor[k], and
													 * c[rs_disk2][p-1] is 0
													 */
					for (i = 0; i < block_size; i++) {
						check_data[data_disk_nbr][k * block_size + i] = tmp_for_xor[k][i];
					}

					m = block_nbr - 1;
					while (0 != m) {
						i = (k + rs_disk1 - rs_disk2 + p) % p;

						for (j = 0; j < block_size; j++) {
							check_data[data_disk_nbr][i * block_size + j] = (char) (tmp_for_xor[i][j] ^ check_data[data_disk_nbr][k
									* block_size + j]);
						}
						k = i;
						m--;
					}
					restarts[data_disk_nbr] = 0;/* restore no. p data */

					Evenodd_decoding(restarts);
				}
			}

			if (restarts[data_disk_nbr + 1] == 1) {
				Evenodd_decoding_1(rs_disk1, rs_disk2);
				STAR_encoding_diag1();
			}
			if (restarts[data_disk_nbr + 2] == 1) {
				Evenodd_decoding(restarts);
				STAR_encoding_diag2();
			}
		}
		// 3 errors
		if (rs_data_nbr == 3) {
			int r, s, t, u, v;

			// store s~~
			char[][] tmp;
			tmp = new char[3][p * block_size];

			char[] tmp_for_s1;
			tmp_for_s1 = new char[block_size];

			char[] tmp_for_s2;
			tmp_for_s2 = new char[block_size];

			char[][] tmp_for_xor;
			tmp_for_xor = new char[p][block_size];

			/* compute s~~ */
			for (i = 0; i < block_nbr; i++) {
				for (j = 0; j < block_size; j++) {
					tmp_for_s1[j] ^= check_data[data_disk_nbr][i * block_size
							+ j];
					tmp_for_s1[j] ^= check_data[data_disk_nbr + 1][i
							* block_size + j];

					tmp_for_s2[j] ^= check_data[data_disk_nbr][i * block_size
							+ j];
					tmp_for_s2[j] ^= check_data[data_disk_nbr + 2][i
							* block_size + j];
				}
			}

			for (i = 0; i < stripe_unit_size; i++) {
				for (j = 0; j <= data_disk_nbr; j++) {
					tmp[0][i] ^= check_data[j][i];
				}
			}

			for (stripe = 0; stripe < block_nbr + 1; stripe++) {
				for (i = 0; i < data_disk_nbr; i++) {
					for (j = 0; j < block_size; j++) {
						k = (stripe - i + p) % p;
						if (k < block_nbr) {
							tmp[1][stripe * block_size + j] ^= check_data[i][k
									* block_size + j];
						}
					}
				}
			}
			for (i = 0; i < block_nbr + 1; i++) {
				for (j = 0; j < block_size; j++) {
					if (i < block_nbr)
						tmp[1][i * block_size + j] ^= (check_data[data_disk_nbr + 1][i
								* block_size + j] ^ tmp_for_s1[j]);
					else
						tmp[1][i * block_size + j] ^= tmp_for_s1[j];
				}
			}

			for (stripe = 0; stripe < block_nbr + 1; stripe++) {
				for (i = 0; i < data_disk_nbr; i++) {
					for (j = 0; j < block_size; j++) {
						k = (stripe + i + p) % p;
						if (k < block_nbr) {
							tmp[2][stripe * block_size + j] ^= check_data[i][k
									* block_size + j];
						}
					}
				}
			}

			for (i = 0; i < block_nbr + 1; i++) {
				for (j = 0; j < block_size; j++) {
					if (i < block_nbr)
						tmp[2][i * block_size + j] ^= (check_data[data_disk_nbr + 2][i
								* block_size + j] ^ tmp_for_s2[j]);
					else
						tmp[2][i * block_size + j] ^= tmp_for_s2[j];
				}
			}
			/* Now,restore s~~ ,put them in tmp */

			r = rs_disk1;
			s = rs_disk2;
			t = rs_disk3;
			u = s - r;
			v = t - s;

			/* deal with 3 error data with symmetry */
			if (u == v) {
				for (i = 0; i < block_nbr + 1; i++) {
					for (j = 0; j < block_size; j++) {
						tmp_for_xor[i][j] = (char) (tmp[0][i * block_size + j]
								^ tmp[0][((t - r + i) % p) * block_size + j]
								^ tmp[1][((t + p + i) % p) * block_size + j] ^ tmp[2][((p
								- r + i) % p)
								* block_size + j]);
					}
				}
				/*
				 * *tmp_for_xor[0] = c[s][0] xor c[s][t-r]tmp_for_xor[1] =
				 * c[s][1] xor c[s][(t-r+1)%p]tmp_for_xor[i] = c[s][i] xor
				 * c[s][(t-r+i)%p] ...................................
				 */
				k = p - 1 - (t - r);/*
									 * c[s][k] xor c[s][p-1] = tmp_for_xor[k],
									 * and c[s][p-1] is 0
									 */
				for (i = 0; i < block_size; i++) {
					check_data[s][k * block_size + i] = tmp_for_xor[k][i];
				}

				m = block_nbr - 1;
				while (0 != m) {
					i = (r - t + k + p) % p;

					for (j = 0; j < block_size; j++) {
						check_data[s][i * block_size + j] = (char) (tmp_for_xor[i][j] ^ check_data[s][k
								* block_size + j]);
					}
					k = i;
					m--;
				}
			}

			/* asymmetry */
			else if (u != v) {
				int d;// d means cross

				char[][] flag;
				flag = new char[3][p];

				for (d = 0; d <= p; d++) {
					if ((u + v * d) % p == 0) {
						break;
					}
				}

				for (i = 0; i < d; i++) {
					// slope -1
					flag[0][(0 + i * v) % p]++;
					flag[1][(s - r + i * v) % p]++;
					flag[2][(t - r + i * v) % p]++;

					// slope 1
					flag[2][(0 + i * v) % p]++;
					flag[1][(t - s + i * v) % p]++;
					flag[0][(t - r + i * v) % p]++;

				}

				int[] count;
				count = new int[p];

				// find how many 1 in line i
				for (i = 0; i < p; i++) {
					for (j = 0; j < 3; j++) {
						if (flag[j][i] == 1)
							count[i]++;
					}
				}

				// a xor a
				for (m = 0; m < block_nbr + 1; m++) {
					for (i = 0; i < p; i++) {
						if (count[i] == 2 || count[i] == 3) {
							for (j = 0; j < block_size; j++)
								tmp_for_xor[m /** v */
								][j] ^= tmp[0][((i + m /** v */
								) % p) * block_size + j];
						}
					}
					for (i = 0; i < d; i++) {
						for (j = 0; j < block_size; j++) {
							tmp_for_xor[m][j] ^= tmp[1][((t + p + 0 + i * v + m) % p)
									* block_size + j];
							tmp_for_xor[m][j] ^= tmp[2][((p - r + 0 + i * v + m) % p)
									* block_size + j];
						}
					}
				}
				/*
				 * now ,*tmp_for_xor[0] = c[s][u] xor c[s][p-u]tmp_for_xor[i] =
				 * c[s][(u + i)%p] xor c[s][ ( p-u+i)%p ]
				 * ...............................................
				 */
				i = u - 1;
				k = (u + i) % p;/*
								 * c[s][k] xor c[s][p-1] = tmp_for_xor[k], and
								 * c[s][p-1] is 0
								 */
				for (j = 0; j < block_size; j++) {
					check_data[s][k * block_size + j] = tmp_for_xor[i][j];
				}

				m = block_nbr - 1;
				while (0 != m) {
					i = (k + u) % p;

					for (j = 0; j < block_size; j++) {
						check_data[s][((u + i) % p) * block_size + j] = (char) (tmp_for_xor[i][j] ^ check_data[s][k
								* block_size + j]);
					}
					k = (u + i) % p;
					m--;
				}
				/* is count[i]==3,then s = s XOR s[0][i] */
				/* next, the value of count[u] & count[p-u] is equal 1 or 2 */
				/*
				 * if((count[u] == 1) && (count[p-u]==1)) { then a[s][u] XOR
				 * a[s][p-u] = s XOR s[1][t] XOR s[2][(p-r)%p] XOR s[1][(t+v)%p]
				 * XOR ...... } else if(count[u] ==2) { then using s[0][u]and
				 * s[0][p-u] }
				 */
			}

			// data s have been restore
			restarts[s] = 0;

			Evenodd_decoding(restarts);
		}
	}

	/**
	 * checksum data:row checksum and slope -1 diagonal line like evenodd
	 */
	void Evenodd_decoding_1(int rs_disk1, int rs_disk2) {
		/*
		 * 2 situations:1,one data error + row data error 2,two data error
		 */
		int i, j, stripe, k;
		char[] tmp;
		tmp = new char[p * block_size];

		char[] tmp_for_s;
		tmp_for_s = new char[block_size];

		/* one data error + row data error */
		if (rs_disk1 < data_disk_nbr && rs_disk2 == data_disk_nbr) {
			for (stripe = 0; stripe < block_nbr + 1; stripe++) {
				for (i = 0; i < data_disk_nbr; i++) {
					for (j = 0; j < block_size; j++) {
						k = (stripe + i + p) % p;
						if (k < block_nbr)
							tmp[stripe * block_size + j] ^= check_data[i][k
									* block_size + j];
					}
				}
			}

			/* find out s */
			stripe = (p - rs_disk1 - 1) % p;

			for (i = 0; i < block_size; i++) {
				if (stripe == p - 1)
					tmp_for_s[i] = tmp[stripe * block_size + i];
				else
					tmp_for_s[i] = (char) (tmp[stripe * block_size + i] ^ check_data[data_disk_nbr + 2][stripe
							* block_size + i]);
			}

			for (i = 0; i < block_nbr; i++) {
				for (j = 0; j < block_size; j++) {
					tmp[i * block_size + j] ^= (tmp_for_s[j] ^ check_data[data_disk_nbr + 2][i
							* block_size + j]);
				}
			}
			for (i = 0; i < block_size; i++)
				tmp[block_nbr * block_size + i] ^= tmp_for_s[i];
			/* now all restored data is store in tmp. */

			for (i = 0; i < p; i++) {
				j = (i + p + rs_disk1) % p;
				if (j < p - 1) {
					System.arraycopy(tmp, i * block_size, check_data[rs_disk1],
							j * block_size, block_size);
				}
			}
			STAR_encoding_row();
		}

		if (rs_disk1 < data_disk_nbr && rs_disk2 < data_disk_nbr) {
			for (i = 0; i < block_nbr; i++) {
				for (j = 0; j < block_size; j++) {
					tmp_for_s[j] ^= check_data[data_disk_nbr][i * block_size
							+ j];
					tmp_for_s[j] ^= check_data[data_disk_nbr + 2][i
							* block_size + j];
				}
			}

			/*
			 * for( i = 0; i < block_nbr; i++) { for( j = 0; j < block_size;
			 * j++) { check_data[p + 1][i * block_size + j] ^= tmp_for_s[j]; } }
			 * compute s
			 */

			for (stripe = 0; stripe < block_nbr + 1; stripe++) {
				for (i = 0; i < data_disk_nbr; i++) {
					for (j = 0; j < block_size; j++) {
						k = (stripe + i + p) % p;
						if (k < block_nbr)
							tmp[stripe * block_size + j] ^= check_data[i][k
									* block_size + j];
					}
				}
			}
			for (i = 0; i < block_nbr; i++) {
				for (j = 0; j < block_size; j++) {
					if (i < block_nbr)
						tmp[i * block_size + j] ^= (check_data[data_disk_nbr + 2][i
								* block_size + j] ^ tmp_for_s[j]);
					// else tmp[ i * block_size + j] ^= tmp_for_s[j];
				}
			}
			for (j = 0; j < block_size; j++)
				tmp[block_nbr * block_size + j] ^= tmp_for_s[j];
			/* s store in tmp */

			stripe = (p - rs_disk1 - 1) % p;

			/* apply tmp_for_s to store temporary data */
			System.arraycopy(tmp, stripe * block_size, tmp_for_s, 0, block_size);

			while (true) {
				/* rs_disk2 */
				k = (stripe + rs_disk2 + p) % p;
				if (k == block_nbr) {
					break;
				}
				System.arraycopy(tmp_for_s, 0, check_data[rs_disk2], k
						* block_size, block_size);

				for (i = 0; i < block_size; i++) {
					for (j = 0; j <= data_disk_nbr; j++)
						if (j != rs_disk1)
							check_data[rs_disk1][k * block_size + i] ^= check_data[j][k
									* block_size + i];
				}

				stripe = (k - rs_disk1 + p) % p;

				for (i = 0; i < block_size; i++) {
					tmp_for_s[i] = (char) (check_data[rs_disk1][k * block_size
							+ i] ^ tmp[stripe * block_size + i]);
				}
			}
		}
	}

	/**
	 * restore data like evenodd
	 */
	void Evenodd_decoding(int[] restarts) {
		int i, j, stripe, k;
		int rs_disk1 = -1;
		int rs_disk2 = -1;
		int rs_nbr = 0;

		for (i = 0; i < data_disk_nbr + 2; i++) {
			if (restarts[i] == 1) {
				rs_disk1 = i;
				rs_nbr++;
				break;
			}
		}
		if (rs_disk1 != -1) {
			for (i = rs_disk1 + 1; i < data_disk_nbr + 2; i++) {
				if (restarts[i] == 1) {
					rs_disk2 = i;
					rs_nbr++;
					break;
				}
			}
		}

		if (rs_disk1 != -1) {
			Arrays.fill(check_data[rs_disk1], (char) 0);
		}
		if (rs_disk2 != -1) {
			Arrays.fill(check_data[rs_disk2], (char) 0);
		}

		// row checksum
		if (rs_disk1 >= data_disk_nbr) {
			if (restarts[data_disk_nbr] == 1)// slope 1
			{
				STAR_encoding_row();
			}
			if (restarts[data_disk_nbr + 1] == 1) {
				STAR_encoding_diag1();
			}
		}

		// one data error + one checksum error
		if (rs_disk1 < data_disk_nbr && rs_nbr == 1) {
			for (i = 0; i < stripe_unit_size; i++) {
				for (j = 0; j <= data_disk_nbr; j++) {
					if (j != rs_disk1) {
						check_data[rs_disk1][i] ^= check_data[j][i];
					}
				}
			}
		}

		// 2 data error
		if (rs_nbr == 2 && rs_disk1 < data_disk_nbr
				&& rs_disk2 >= data_disk_nbr) {
			if (rs_disk2 == data_disk_nbr + 1) {
				// computing s firstly
				for (i = 0; i < stripe_unit_size; i++) {
					for (j = 0; j <= data_disk_nbr; j++) {
						if (j != rs_disk1)
							check_data[rs_disk1][i] ^= check_data[j][i];
					}
				}
				STAR_encoding_diag1();
			}

			if (rs_disk2 == data_disk_nbr) {
				/* int i,j,stripe,k; */

				char[] tmp;
				tmp = new char[p * block_size];

				char[] tmp_for_s;
				tmp_for_s = new char[block_size];

				for (stripe = 0; stripe < block_nbr + 1; stripe++) {
					for (i = 0; i < data_disk_nbr; i++) {
						for (j = 0; j < block_size; j++) {
							k = (stripe - i + p) % p;
							if (k < block_nbr)
								tmp[stripe * block_size + j] ^= check_data[i][k
										* block_size + j];
						}
					}
				}

				stripe = (rs_disk1 + p - 1) % p;
				for (i = 0; i < block_size; i++) {
					if (stripe == p - 1)
						tmp_for_s[i] = tmp[stripe * block_size + i];
					else
						tmp_for_s[i] = (char) (tmp[stripe * block_size + i] ^ check_data[data_disk_nbr + 1][stripe
								* block_size + i]);
				}

				for (i = 0; i < block_nbr; i++) {
					for (j = 0; j < block_size; j++) {
						tmp[i * block_size + j] ^= (tmp_for_s[j] ^ check_data[data_disk_nbr + 1][i
								* block_size + j]);
					}
				}
				for (j = 0; j < block_size; j++)
					tmp[block_nbr * block_size + j] ^= tmp_for_s[j];

				for (i = 0; i < p; i++) {
					j = (i + p - rs_disk1) % p;
					if (j < p - 1) {
						System.arraycopy(tmp, i * block_size,
								check_data[rs_disk1], j * block_size,
								block_size);
						/*********************/
					}
				}

				STAR_encoding_row();
			}
		}

		if (rs_nbr == 2 && rs_disk1 < data_disk_nbr && rs_disk2 < data_disk_nbr)/* 两个数据盘出错 */
		{

			char[] tmp;
			tmp = new char[p * block_size];

			char[] tmp_for_s;

			tmp_for_s = new char[block_size];

			for (i = 0; i < block_nbr; i++) {
				for (j = 0; j < block_size; j++) {
					tmp_for_s[j] ^= check_data[data_disk_nbr][i * block_size
							+ j];
					tmp_for_s[j] ^= check_data[data_disk_nbr + 1][i
							* block_size + j];
				}
			}

			for (stripe = 0; stripe < block_nbr + 1; stripe++) {
				for (i = 0; i < data_disk_nbr; i++) {
					k = (stripe - i + p) % p;
					if (k < block_nbr)
						for (j = 0; j < block_size; j++) {
							tmp[stripe * block_size + j] ^= check_data[i][k
									* block_size + j];
						}
				}
			}
			for (i = 0; i < block_nbr; i++) {
				for (j = 0; j < block_size; j++) {
					tmp[i * block_size + j] ^= (check_data[data_disk_nbr + 1][i
							* block_size + j] ^ tmp_for_s[j]);
				}
			}
			for (j = 0; j < block_size; j++)
				tmp[block_nbr * block_size + j] ^= tmp_for_s[j];
			/* store s~ in tmp */

			stripe = (rs_disk1 + p - 1) % p;/* rs_disk1未参与第stripe个条纹的对角线校验 */
			System.arraycopy(tmp, stripe * block_size, tmp_for_s, 0, block_size);
			while (true) {
				k = (stripe - rs_disk2 + p) % p;/* 第rs_disk2个数据盘的第k块在第stripe个条纹 */
				if (k == block_nbr)
					break;

				System.arraycopy(tmp_for_s, 0, check_data[rs_disk2], k
						* block_size, block_size);

				for (j = 0; j < block_size; j++) {
					for (i = 0; i <= data_disk_nbr; i++)
						if (i != rs_disk1)
							check_data[rs_disk1][k * block_size + j] ^= check_data[i][k
									* block_size + j];
				}

				stripe = (k + rs_disk1 + p) % p;

				for (j = 0; j < block_size; j++) {
					tmp_for_s[j] = (char) (check_data[rs_disk1][k * block_size
							+ j] ^ tmp[stripe * block_size + j]);
				}

			}
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		char xx = (char) 0 ^ 'a' ^ 'b' ^ '' ^ 'd';
		out.printf("%c\n", xx);
		out.printf("starting");

		final int NUM = 7;
		int[] err = new int[NUM];
		Star starItem = new Star();
		// Star starItem = new Star(6,257,1024);

		starItem.setData();
		starItem.encoding();
		starItem.outputData();

		// 1 means fault data
		for (int i = 0; i < NUM; i++) {
			err[i] = 0;
		}

		// err[0]=1;
		// err[1]=1;
		err[2] = 1;
		// err[3]=1;
		err[4] = 1;
		// err[5]=1;
		err[6] = 1;

		starItem.setErrData(err);
		starItem.decoding();
		starItem.outputOrigin();

	}

}
