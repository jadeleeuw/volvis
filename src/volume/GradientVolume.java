/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

/**
 *
 * @author michel
 *  Modified by Anna Vilanova
 */
public class GradientVolume {

	
	
	//////////////////////////////////////////////////////////////////////
	///////////////// TO BE IMPLEMENTED //////////////////////////////////
	//////////////////////////////////////////////////////////////////////

    /**
     * Computes the gradients for each voxel and adds them using setGradient
     */
    private void compute() {
//        Iterate over all the voxels by iterating over x, y, and z.
        for (int x = 0; x < dimX; x++) {
            for (int y = 0; y < dimY; y++) {
                for (int z = 0; z < dimZ; z++) {
//                    Check if it is a voxel along the edge/sides of the model and set them to zero.
                    if (x == 0 || x == dimX-1 || y == 0 || y == dimY-1 || z == 0 || z == dimZ-1) {
                        setGradient(x, y, z, zero);
                    }
                    else {
//                       Compare this voxel to the 6 neighbouring voxels. The difference in each dimension forms the
//                       VoxelGradient
                        float gx = (volume.getVoxel(x+1, y, z) - volume.getVoxel(x-1, y, z)) / 2;
                        float gy = (volume.getVoxel(x, y+1, z) - volume.getVoxel(x, y-1, z)) / 2;
                        float gz = (volume.getVoxel(x, y, z+1) - volume.getVoxel(x, y, z-1)) / 2;

                        //Set VoxelGradient
                        setGradient(x, y, z, new VoxelGradient(gx, gy, gz));
                    }
                }
            }
        }
    }

    /**
     * Interpolate between two VoxelGradients with a given factor.
     * @param g0 First VoxelGradient
     * @param g1 Second VoxelGradient
     * @param factor factor
     * @param result The values of the interpolation will be put into this VoxelGradient
     */
    private void interpolate(VoxelGradient g0, VoxelGradient g1, float factor, VoxelGradient result) {
//        Calculate the (1-f)
        float invFac = (1f - factor);
//        Interpolate over each dimension
        result.x = factor * g1.x + invFac * g0.x;
        result.y = factor * g1.y + invFac * g0.y;
        result.z = factor * g1.z + invFac * g0.z;
//        Calculate the magnitude using the constructor of VoxelGradient
        result.mag = new VoxelGradient(result.x, result.y, result.z).mag;
    }

    /**
     * Interpolates the voxels around the given coordinate to calculate the value of the coord.
     * @param coord Coordinate for which we want a value.
     * @return The value calculated using tri-linear interpolation.
     */
    public VoxelGradient getGradient(double[] coord) {
//        Check whether the coordinate is within the model.
        if (coord[0] < 0 || coord[0] > (dimX-2) || coord[1] < 0 || coord[1] > (dimY-2)
                || coord[2] < 0 || coord[2] > (dimZ-2)) {
            return zero;
        }

//        Get the coordinate values of the voxels around the given coordinates.
        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);

//        Retrieve the VoxelGradients of the 8 surrounding voxel gradients.
        VoxelGradient a = getGradient(x, y, z);
        VoxelGradient b = getGradient(x, y, z+1);
        VoxelGradient c = getGradient(x, y+1, z);
        VoxelGradient d = getGradient(x, y+1, z+1);
        VoxelGradient e = getGradient(x+1, y, z);
        VoxelGradient f = getGradient(x+1, y, z+1);
        VoxelGradient g = getGradient(x+1, y+1, z);
        VoxelGradient h = getGradient(x+1, y+1, z+1);

//        Apply bilinear interpolation on the x plane
        VoxelGradient ab = new VoxelGradient();
        interpolate(a, b, (float) (coord[2]-z), ab);
        VoxelGradient cd = new VoxelGradient();
        interpolate(c, d, (float) (coord[2]-z), cd);
        VoxelGradient abcd = new VoxelGradient();
        interpolate(ab, cd, (float) (coord[1]-y), abcd);

//        Apply bilinear interpolation on the x+1 plane
        VoxelGradient ef = new VoxelGradient();
        interpolate(e, f, (float) (coord[2]-z), ef);
        VoxelGradient gh = new VoxelGradient();
        interpolate(g, h, (float) (coord[2]-z), gh);
        VoxelGradient efgh = new VoxelGradient();
        interpolate(ef, gh, (float) (coord[1]-y), efgh);

//        Interpolate the two bilinear interpolation results to get the final result.
        VoxelGradient result = new VoxelGradient();
        interpolate(abcd, efgh, (float) (coord[0]-x), result);
        return result;
    }
    
    
    
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
	
    //Returns the maximum gradient magnitude
    //
    //The data array contains all the gradients, in this function you have to return the maximum magnitude of the vectors in data[] 
 
    //Do NOT modify this function
    public double getMaxGradientMagnitude() {
        if (maxmag >= 0) {
            return maxmag;
        } else {
            double magnitude = data[0].mag;
            for (int i=0; i<data.length; i++) {
                if (data[i].mag > magnitude) {
                    magnitude = data[i].mag;
                }
            }   
            maxmag = magnitude;
            return magnitude;
        }
    }
    	
	
	
	//Do NOT modify this function
	public GradientVolume(Volume vol) {
        volume = vol;
        dimX = vol.getDimX();
        dimY = vol.getDimY();
        dimZ = vol.getDimZ();
        data = new VoxelGradient[dimX * dimY * dimZ];
        compute();
        maxmag = -1.0;
    }

	//Do NOT modify this function
	public VoxelGradient getGradient(int x, int y, int z) {
        return data[x + dimX * (y + dimY * z)];
    }

  
	//Do NOT modify this function: Basically calculates the Nearest Neighbor interpolation for the gradient
    public VoxelGradient getGradient2(double[] coord) {
        if (coord[0] < 0 || coord[0] > (dimX-2) || coord[1] < 0 || coord[1] > (dimY-2)
                || coord[2] < 0 || coord[2] > (dimZ-2)) {
            return zero;
        }

        int x = (int) Math.round(coord[0]);
        int y = (int) Math.round(coord[1]);
        int z = (int) Math.round(coord[2]);
        return getGradient(x, y, z);
    }

	//Do NOT modify this function
    public void setGradient(int x, int y, int z, VoxelGradient value) {
        data[x + dimX * (y + dimY * z)] = value;
    }

	//Do NOT modify this function
    public void setVoxel(int i, VoxelGradient value) {
        data[i] = value;
    }
    
	//Do NOT modify this function
    public VoxelGradient getVoxel(int i) {
        return data[i];
    }
    
	//Do NOT modify this function
    public int getDimX() {
        return dimX;
    }
    
	//Do NOT modify this function
    public int getDimY() {
        return dimY;
    }
    
	//Do NOT modify this function
    public int getDimZ() {
        return dimZ;
    }

	//Do NOT modify this attributes
    private int dimX, dimY, dimZ;
    private VoxelGradient zero = new VoxelGradient();
    VoxelGradient[] data;
    Volume volume;
    double maxmag;
    
    //If needed add new attributes here:
}
