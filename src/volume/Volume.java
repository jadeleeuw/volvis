/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author michel
 *  Modified by Anna Vilanova
 */
public class Volume {
    
	//////////////////////////////////////////////////////////////////////
	///////////////// TO BE IMPLEMENTED //////////////////////////////////
		//////////////////////////////////////////////////////////////////////

    //This function linearly interpolates the value g0 and g1 given the factor (t) 
    //the result is returned. You can use it to tri-linearly interpolate the values 
	private float interpolate(float g0, float g1, float factor) {
            return factor*g1 + g0*(1f - factor);
//            float result=0;
//            // to be implemented
//            return result; 
    }
	
	//You have to implement the trilinear interpolation of the volume
	//First implement the interpolated function above
        // At the moment the function does takes just the lowest voxel value
        // to trilinear interpolation
	public short getVoxelLinearInterpolate(double[] coord) {
        if (coord[0] < 0 || coord[0] > (dimX-2) || coord[1] < 0 || coord[1] > (dimY-2)
                || coord[2] < 0 || coord[2] > (dimZ-2)) {
            return 0;
        }
        /* notice that in this framework we assume that the distance between neighbouring voxels is 1 in all directions*/

        // Cellecting vowel values for the eight voxels serounding our coordinat.
        int floor0 = (int) Math.floor(coord[0]);
        int ceil0 = (int) Math.ceil(coord[0]);
        int floor1 = (int) Math.floor(coord[1]);
        int ceil1 = (int) Math.ceil(coord[1]);
        int floor2 = (int) Math.floor(coord[2]);
        int ceil2 = (int) Math.ceil(coord[2]);
        
        
        float a = getVoxel(floor0, floor1, floor2);
        float b = getVoxel(floor0, floor1, ceil2);
        float c = getVoxel(floor0, ceil1, floor2);
        float d = getVoxel(floor0, ceil1, ceil2);
        float e = getVoxel(ceil0, floor1, floor2);
        float f = getVoxel(ceil0, floor1, ceil2);
        float g = getVoxel(ceil0, ceil1, floor2);
        float h = getVoxel(ceil0, ceil1, ceil2);

        //biliniear interpolation on plane Math.floor(coord[0])
        float ab = interpolate(a, b, (float) (coord[2]-floor2));
        float cd = interpolate(c, d, (float) (coord[2]-floor2));
        float abcd = interpolate(ab, cd, (float) (coord[1]-floor1));
        
        //biliniear interpolation on plane Math.ceil(coord[0])
        float ef = interpolate(e, f, (float) (coord[2]-floor2));
        float gh = interpolate(g, h, (float) (coord[2]-floor2));
        float efgh = interpolate(ef, gh, (float) (coord[1]-floor1));
            
        return (short) Math.round(interpolate(abcd, efgh, (float) (coord[0]-floor0))); 
    }
		
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////

	//Do NOT modify this function
        // This function is an example and does a nearest neighbour interpolation
	public short getVoxelNN(double[] coord) {
        if (coord[0] < 0 || coord[0] > (dimX-1) || coord[1] < 0 || coord[1] > (dimY-1)
                || coord[2] < 0 || coord[2] > (dimZ-1)) {
            return 0;
        }
        /* notice that in this framework we assume that the distance between neighbouring voxels is 1 in all directions*/
        int x = (int) Math.round(coord[0]); 
        int y = (int) Math.round(coord[1]);
        int z = (int) Math.round(coord[2]);
    
        return getVoxel(x, y, z);
    }
	
	//Do NOT modify this function
    public Volume(int xd, int yd, int zd) {
        data = new short[xd*yd*zd];
        dimX = xd;
        dimY = yd;
        dimZ = zd;
        this.maximum = calculateMaximum();
    }
	//Do NOT modify this function
    public Volume(File file) {
        
        try {
            VolumeIO reader = new VolumeIO(file);
            dimX = reader.getXDim();
            dimY = reader.getYDim();
            dimZ = reader.getZDim();
            data = reader.getData().clone();
            this.maximum = calculateMaximum();
            computeHistogram();
        } catch (IOException ex) {
            System.out.println("IO exception");
        }
        
    }
    
	//Do NOT modify this function
    public short getVoxel(int x, int y, int z) {
        return data[x + dimX*(y + dimY * z)];
    }
    
	//Do NOT modify this function
    public void setVoxel(int x, int y, int z, short value) {
    	int i = x + dimX*(y + dimY * z);
        data[i] = value;
    }
    
	//Do NOT modify this function
    public void setVoxel(int i, short value) {
        data[i] = value;
    }
    
	//Do NOT modify this function
    public short getVoxel(int i) {
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

	//Do NOT modify this function
    public short getMinimum() {
        short minimum = data[0];
        for (int i=0; i<data.length; i++) {
            minimum = data[i] < minimum ? data[i] : minimum;
        }
        return minimum;
    }
    
	//Do NOT modify this function
    public short getMaximum() {
        return this.maximum;
    }
    
    private short calculateMaximum() {
        short maximum = data[0];
        for (int i=0; i<data.length; i++) {
            maximum = data[i] > maximum ? data[i] : maximum;
        }
        return maximum;
    }
 
	//Do NOT modify this function
    public int[] getHistogram() {
        return histogram;
    }
    
	//Do NOT modify this function
    private void computeHistogram() {
        histogram = new int[getMaximum() + 1];
        for (int i=0; i<data.length; i++) {
            histogram[data[i]]++;
        }
    }
    
	//Do NOT modify these attributes
    private int dimX, dimY, dimZ;
    private short[] data;
    private int[] histogram;
    private short maximum;
}