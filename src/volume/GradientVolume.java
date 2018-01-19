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
	
	//Compute the gradient of contained in the volume attribute and save it into the data attribute
	//
	//This is a lengthy computation and is performed only once (have a look at the constructor GradientVolume) 

    // You need to implement this function
    private void compute() {
        for (int x = 0; x < dimX; x++) {
            for (int y = 0; y < dimY; y++) {
                for (int z = 0; z < dimZ; z++) {
                    if (x == 0 || x == dimX-1 || y == 0 || y == dimY-1 || z == 0 || z == dimZ-1) {
                        setGradient(x, y, z, zero);
                    } else {
                        float gx = (volume.getVoxel(x+1, y, z) - volume.getVoxel(x-1, y, z)) / 2;
                        float gy = (volume.getVoxel(x, y+1, z) - volume.getVoxel(x, y-1, z)) / 2;
                        float gz = (volume.getVoxel(x, y, z+1) - volume.getVoxel(x, y, z-1)) / 2;

                        setGradient(x, y, z, new VoxelGradient(gx, gy, gz));
                    }
                }
            }
        }
    }
    	
    //You need to implement this function
    //This function linearly interpolates gradient vector gas0 and g1 given the factor (t) 
    //the resut is given at result. You can use it to tri-linearly interpolate the gradient
    private void interpolate(VoxelGradient g0, VoxelGradient g1, float factor, VoxelGradient result) {
       // to be implemented
       result.x = factor*g1.x + (1f - factor)* g0.x;
       result.y = factor*g1.y + (1f - factor)* g0.y;
       result.z = factor*g1.z + (1f - factor)* g0.z ;
       result.mag = new VoxelGradient(result.x, result.y, result.z).mag;
    }
    
    // You need to implement this function
    // This function returns the gradient at position coord using trilinear interpolation
    public VoxelGradient getGradient(double[] coord) {

        if (coord[0] < 0 || coord[0] > (dimX-2) || coord[1] < 0 || coord[1] > (dimY-2)
                || coord[2] < 0 || coord[2] > (dimZ-2)) {
            return zero;
        }
        
        float x = (float) Math.floor(coord[0]);
        float y = (float) Math.floor(coord[1]);
        float z = (float) Math.floor(coord[2]);
        
        VoxelGradient a = getGradient(x, y, z);
        VoxelGradient b = getGradient(x, y, z+1);
        VoxelGradient c = getGradient(x, y+1, z);
        VoxelGradient d = getGradient(x, y+1, z+1);
        VoxelGradient e = getGradient(x+1, y, z);
        VoxelGradient f = getGradient(x+1, y, z+1);
        VoxelGradient g = getGradient(x+1, y+1, z);
        VoxelGradient h = getGradient(x+1, y+1, z+1);

        //biliniear interpolation on plane x)
        VoxelGradient ab = new VoxelGradient();
        interpolate(a, b, (float) (coord[2]-z), ab);
        VoxelGradient cd = new VoxelGradient();
        interpolate(c, d, (float) (coord[2]-z), cd);
        VoxelGradient abcd = new VoxelGradient();
        interpolate(ab, cd, (float) (coord[1]-y), abcd);
        
        //biliniear interpolation on plane x+1)
        VoxelGradient ef = new VoxelGradient();
        interpolate(e, f, (float) (coord[2]-z), ef);
        VoxelGradient gh = new VoxelGradient();
        interpolate(g, h, (float) (coord[2]-z), gh);
        VoxelGradient efgh = new VoxelGradient();
        interpolate(ef, gh, (float) (coord[1]-y), efgh);
        
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
                magnitude = data[i].mag > magnitude ? data[i].mag : magnitude;
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
