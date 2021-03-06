// Generated by Structorizer 3.30-08 

// Copyright (C) 2020-03-21 Kay Gürtzig 
// License: GPLv3-link 
// GNU General Public License (V 3) 
// https://www.gnu.org/licenses/gpl.html 
// http://www.gnu.de/documents/gpl.de.html 

import java.util.Scanner;

/**
 * Computes the sum and average of the numbers read from a user-specified
 * text file (which might have been created via generateRandomNumberFile(4)).
 * 
 * This program is part of an arrangement used to test group code export (issue
 * #828) with FileAPI dependency.
 * The input check loop has been disabled (replaced by a simple unchecked input
 * instruction) in order to test the effect of indirect FileAPI dependency (only the
 * called subroutine directly requires FileAPI now).
 */
public class ComputeSum {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// TODO: Check and accomplish variable declarations: 
		???[] values;
		double sum;
		int nValues;
		??? file_name;
		int fileNo;

		// TODO: You may have to modify input instructions, 
		//       e.g. by replacing nextLine() with a more suitable call 
		//       according to the variable type, say nextInt(). 

		fileNo = 1000;
		// Disable this if you enable the loop below! 
		System.out.print("Name/path of the number file"); file_name = (new Scanner(System.in)).nextLine();
		// If you enable this loop, then the preceding input instruction is to be disabled 
		// and the fileClose instruction in the alternative below is to be enabled. 
// 		do { 
// 			System.out.print("Name/path of the number file"); file_name = (new Scanner(System.in)).nextLine(); 
// 			fileNo = fileOpen(file_name); 
// 		} while (! (fileNo > 0 || file_name == "")); 
		if (fileNo > 0) {
			// This should be enabled if the input check loop above gets enabled. 
// 			fileClose(fileNo); 
			values = new ???[]{};
			nValues = 0;
			try {
				nValues = readNumbers(file_name, values, 1000);
			}
			catch (Exception exe8751d56) {
				String failure = exe8751d56.getMessage()
				System.out.println(failure);
				System.exit(-7)
			}
			sum = 0.0;
			for (int k = 0; k <= nValues-1; k += (1)) {
				sum = sum + values[k];
			}
			System.out.println(("sum = ") + (sum));
			System.out.println(("average = ") + (sum / nValues));
		}
	}

	/**
	 * Tries to read as many integer values as possible upto maxNumbers
	 * from file fileName into the given array numbers.
	 * Returns the number of the actually read numbers. May cause an exception.
	 * @param fileName
	 * @param numbers
	 * @param maxNumbers
	 * @return 
	 */
	private static int readNumbers(String fileName, int[] numbers, int maxNumbers) {
		// TODO: Check and accomplish variable declarations: 
		int number;
		int nNumbers;
		int fileNo;

		nNumbers = 0;
		fileNo = fileOpen(fileName);
		if (fileNo <= 0) {
			throw "File could not be opened!";
		}
		try {
			while (! fileEOF(fileNo) && nNumbers < maxNumbers) {
				number = fileReadInt(fileNo);
				numbers[nNumbers] = number;
				nNumbers = nNumbers + 1;
			}
		}
		catch (Exception exbce25944) {
			String error = exbce25944.getMessage()
			throw exbce25944;
		}
		finally {
			fileClose(fileNo);
		}
		return nNumbers;
	}

}
