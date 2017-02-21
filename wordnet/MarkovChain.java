package wordnet;
import Jama.Matrix;
import Jama.EigenvalueDecomposition;

public class MarkovChain {

    public static void main(String[] args) { 

        // the state transition matrix
        int N = 7;
        double[][] transition = { { 0.386, 0.147, 0.202, 0.062, 0.140, 0.047, 0.016},
                                  { 0.107, 0.267, 0.227, 0.120, 0.207, 0.052, 0.020},
                                  { 0.035, 0.101, 0.188, 0.191, 0.357, 0.067, 0.061},
                                  { 0.021, 0.039, 0.112, 0.212, 0.431, 0.124, 0.061},
                                  { 0.009, 0.024, 0.075, 0.123, 0.473, 0.171, 0.125},
                                  { 0.000, 0.103, 0.041, 0.088, 0.301, 0.312, 0.155},
                                  { 0.000, 0.008, 0.036, 0.083, 0.364, 0.235, 0.274}
                                };

//        System.out.println(transition[2][1]);
        // compute using 50 iterations of power method
        Matrix A = new Matrix(transition);
        A.print(7, 2);
        A = A.transpose();
        Matrix x = new Matrix(N, 1, 1.0 / N); // initial guess for eigenvector
        for (int i = 0; i < 50; i++) {
            x = A.times(x);
            x = x.times(1.0 / x.norm1());       // rescale
        }
        double [][] getM = x.getArray();
//        System.out.println(getM[1]);
        x.print(9, 6);



        // compute by finding eigenvector corresponding to eigenvalue = 1
//        EigenvalueDecomposition eig = new EigenvalueDecomposition(A);
//        Matrix V = eig.getV();
//        double[] real = eig.getRealEigenvalues();
//        for (int i = 0; i < N; i++) {
//            if (Math.abs(real[i] - 1.0) < 1E-6) {
//                x = V.getMatrix(0, N-1, i, i);
//                x = x.times(1.0 / x.norm1());
//                x.print(9, 6);
//            }
//        }
//
//        // If ergordic, stationary distribution = unique solution to Ax = x
//        // up to scaling factor.
//        // We solve (A - I) x = 0, but replace row 0 with constraint that
//        // says the sum of x coordinates equals one
//        Matrix B = A.minus(Matrix.identity(N, N));
//        for (int j = 0; j < N; j++)
//            B.set(0, j, 1.0);
//        Matrix b = new Matrix(N, 1);
//        b.set(0, 0, 1.0);
//        x = B.solve(b);
//        x.print(9, 6);
    }

}