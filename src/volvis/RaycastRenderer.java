/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gui.RaycastRendererPanel;
import gui.TransferFunction2DEditor;
import gui.TransferFunctionEditor;
import util.TFChangeListener;
import util.VectorMath;
import volume.GradientVolume;
import volume.Volume;
import volume.VoxelGradient;

import java.awt.image.BufferedImage;


/**
 *
 * @author michel
 *  Edit by Anna Vilanova & Nicola Pezzotti
 */

// This is a very important class where you have to implement most of your work

public class RaycastRenderer extends Renderer implements TFChangeListener {

	
	//////////////////////////////////////////////////////////////////////
	///////////////// TO BE IMPLEMENTED //////////////////////////////////
	//////////////////////////////////////////////////////////////////////

	//in this function we update the "image" attribute using the slicing technique
    void slicer(double[] viewMatrix) {
	    // we start by clearing the image
	    resetImage();
	
	    // vector uVec and vVec define the view plane, 
	    // perpendicular to the view vector viewVec which is going from the view point towards the object
	    double[] viewVec = new double[3];
	    double[] uVec = new double[3];
	    double[] vVec = new double[3];
	    getViewPlaneVectors(viewMatrix,viewVec,uVec,vVec);
	  
	    // compute the volume center
	    double[] volumeCenter = new double[3];
	    computeVolumeCenter(volumeCenter);
	    
	    // Here will be stored the 3D coordinates of every pixel in the plane 
	    double[] pixelCoord = new double[3];
	   	
	    // sample on a plane through the origin of the volume data
	    double max = volume.getMaximum();
	    TFColor voxelColor = new TFColor();
            TFColor colorAux;
	    //Iterate on every pixel
	    for (int j = 0; j < image.getHeight(); j++) {
	        for (int i = 0; i < image.getWidth(); i++) {
	        	//pixelCoord now contains the 3D coordinates for pixel (i,j)
	        	computePixelCoordinates(pixelCoord,volumeCenter,uVec,vVec,i,j);
		            
	            //pixelCoord now contains the 3D coordinates of the pixels (i,j)
	            //we now have to get the value for the in the 3D volume for the pixel
	            //we can use a nearest neighbor implementation like this:
	            int val = volume.getVoxelNN(pixelCoord);

	            		
	            //you have to implement the function getVoxelLinearInterpolated in Volume.java
	            //in order to complete the assignment
	            //int val = volume.getVoxelLinearInterpolate(pixelCoord); //and then use this line
	            
	            
	            // Map the intensity to a grey value by linear scaling
	            voxelColor.r = val/max;
	            voxelColor.g = voxelColor.r;
	            voxelColor.b = voxelColor.r;
	            
	            // the following instruction makes intensity 0 completely transparent and the rest opaque
	               voxelColor.a = val > 0 ? 1.0 : 0.0;   
                       
	            // Alternatively, apply the transfer function to obtain a color using the tFunc attribute
	            //colorAux== tFunc.getColor(val);
                    //voxelColor.r=colorAux.r;voxelColor.g=colorAux.g;voxelColor.b=colorAux.b;voxelColor.a=colorAux.a; 
                    // You can also simply use voxelColor = tFunc.getColor(val); However then you copy by reference and this means that if you change 
                    // voxelColor you will be actually changing the transfer function
	            
	            //BufferedImage expects a pixel color packed as ARGB in an int
	            //use the function computeImageColor to convert your double color in the range 0-1 to the format need by the image
	            int pixelColor = computeImageColor(voxelColor.r,voxelColor.g,voxelColor.b,voxelColor.a);
	            image.setRGB(i, j, pixelColor);
	        }
	    }
	}
    

  
    //Implementation of the MIP per ray  given the entry and exit point and the ray direction
    // sampleStep indicates the distance between samples
    // To be implemented
    int traceRayMIP(double[] entryPoint, double[] exitPoint, double[] rayVector, double sampleStep) {
    	//Hint: compute the increment and the number of samples you need and iterate over them.

        //You need to iterate through the ray. Starting at the entry point.

        double[] step = new double[3];
        VectorMath.setVector(step, exitPoint[0]-entryPoint[0], exitPoint[1]-entryPoint[1], exitPoint[2]-entryPoint[2]);

        double d = VectorMath.length(step);
        double steps = d / sampleStep;

        for (int i=0; i<3; i++) {
            step[i] /= steps;
        }

        double maxIntensity = Double.MIN_VALUE;
        double[] temp = entryPoint.clone();

        for(int i = 0; i < steps; i++){
            short value = volume.getVoxelLinearInterpolate(temp);
            if(value > maxIntensity) {
                maxIntensity = value;
            }
            else if(maxIntensity==volume.getMaximum()) {
                break; //this check makes sure that when the maximum possible intensity is reached, the search stops
            }

            VectorMath.setVector(temp, temp[0]+step[0], temp[1]+step[1], temp[2]+step[2]);
        }

        // Example color, you have to substitute it by the result of the MIP
        double c = maxIntensity / volume.getMaximum();
        double alpha = c > 0 ? 1.0 : 0.0;
        return computeImageColor(c,c,c,alpha);
    }

    int traceRayComposite(double[] entryPoint, double[] exitPoint, double[] rayVector, double sampleStep) {
        double[] lightVector = new double[3];
        double[] halfVector = new double[3];
        //the light vector is directed toward the view point (which is the source of the light)
        //half vector is used to speed up the phong shading computation see slides
        getLightVector(lightVector,halfVector,rayVector);
        
        // You need to implement the rest of the function for compositing.

        double[] step = new double[3];
        VectorMath.setVector(step, exitPoint[0]-entryPoint[0], exitPoint[1]-entryPoint[1], exitPoint[2]-entryPoint[2]);
        
        double d = VectorMath.length(step);
        double steps = d / sampleStep;

        for (int i=0; i<3; i++) {
            step[i] /= steps;
        }
        
        double r = 0, g = 0, b = 0, a = 0;
        double ka = 0.1, kd = 0.7, ks = 0.2;
        int n = 10;
        
        double[] temp = exitPoint.clone();
        double value;
        TFColor colour = new TFColor();
        
        for(int i = 0; i < steps-1; i++){
            value = volume.getVoxelLinearInterpolate(temp);
            VoxelGradient gradient =  gradients.getGradient(temp);
            float gm = gradient.mag;
           
            if (compositingMode) {
                colour = tFunc.getColor((int) value);
            }

            if (tf2dMode) {
                colour.a = tFunc2D.color.a;
                colour.r = tFunc2D.color.r;
                colour.g = tFunc2D.color.g;
                colour.b = tFunc2D.color.b;
                
                int fv = tFunc2D.baseIntensity;
                double rad = tFunc2D.radius;
                if (value == fv && gm == 0d) {
                    colour.a *= 1d;
                } else if(gm > 0d && value - (rad * gm) <= fv && fv <= value + (rad * gm)) {
                    colour.a *= 1d - (1d/rad) * Math.abs((fv - value)/gm);
                } else {
                    colour.a *= 0d;
                }
            }

            if (shadingMode) {
                double[] gradientVec = new double[3];
                VectorMath.setVector(gradientVec, gradient.x/gm, gradient.y/gm, gradient.z/gm);

                double diffFactor = VectorMath.dotproduct(gradientVec,lightVector) * kd;
                double specFactor = Math.pow(VectorMath.dotproduct(gradientVec,halfVector), n) * ks;

                if (diffFactor  > 0) {
                    colour.r = colour.r * diffFactor + ka * colour.r;
                    colour.g = colour.g * diffFactor + ka * colour.g;
                    colour.b = colour.b * diffFactor + ka * colour.b;
                }
                if (specFactor > 0) {
                    colour.r += specFactor;
                    colour.g += specFactor;
                    colour.b += specFactor;
                }
            }
            r = colour.a * colour.r + (1 - colour.a) * r;
            g = colour.a * colour.g + (1 - colour.a) * g;
            b = colour.a * colour.b + (1 - colour.a) * b;
            a = colour.a + (1 - colour.a) * a;

            if(a>0.99){
                break; // If the value reaches 1, the next opacity will be of less value.
            }
            VectorMath.setVector(temp, temp[0]-step[0], temp[1]-step[1], temp[2]-step[2]);

        }
        
        return computeImageColor(r,g,b,a);
    }
    
    void raycast(double[] viewMatrix) {

    	//data allocation
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        double[] pixelCoord = new double[3];
        double[] entryPoint = new double[3];
        double[] exitPoint = new double[3];

        // ray parameters
        int increment = 1;
        double sampleStep = 1.0;
      
        
        // reset the image to black
        resetImage();
        // compute the view plane and the view vector that is used to compute the entry and exit point of the ray the viewVector is pointing towards the camera
        getViewPlaneVectors(viewMatrix,viewVec,uVec,vVec);
        
        //The ray is pointing towards the scene
        double[] rayVector = new double[3];
        rayVector[0]=-viewVec[0];rayVector[1]=-viewVec[1];rayVector[2]=-viewVec[2];
        
        // We use orthographic projection. Viewer is far away at the infinite, all pixels have the same rayVector.
        
        // ray computation for each pixel
        for (int j = 0; j < image.getHeight(); j += increment) {
            for (int i = 0; i < image.getWidth(); i += increment) {
                // compute starting points of rays in a plane shifted backwards to a position behind the data set
            	computePixelCoordinatesBehind(pixelCoord,viewVec,uVec,vVec,i,j);
            	// compute the entry and exit point of the ray
                computeEntryAndExit(pixelCoord, rayVector, entryPoint, exitPoint);
                if ((entryPoint[0] > -1.0) && (exitPoint[0] > -1.0)) {
                    int val = 0;
                    if (compositingMode || tf2dMode) {
                        val = traceRayComposite(entryPoint, exitPoint, rayVector, sampleStep);
                    } else if (mipMode) {
                        val = traceRayMIP(entryPoint, exitPoint, rayVector, sampleStep);
                    }
                    for (int ii = i; ii < i + increment; ii++) {
                        for (int jj = j; jj < j + increment; jj++) {
                            image.setRGB(ii, jj, val);
                        }
                    }
                }

            }
        }
    }
		
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	
    private Volume volume = null;
    private GradientVolume gradients = null;
    RaycastRendererPanel panel;
    TransferFunction tFunc;
    TransferFunction2D tFunc2D;
    TransferFunctionEditor tfEditor;
    TransferFunction2DEditor tfEditor2D;
    private boolean mipMode = false;
    private boolean slicerMode = true;
    private boolean compositingMode = false;
    private boolean tf2dMode = false;
    private boolean shadingMode = false;

    //Do NOT modify this function
    int computeImageColor(double r, double g, double b, double a){
		int c_alpha = 	a <= 1.0 ? (int) Math.floor(a * 255) : 255;
        int c_red = 	r <= 1.0 ? (int) Math.floor(r * 255) : 255;
        int c_green = 	g <= 1.0 ? (int) Math.floor(g * 255) : 255;
        int c_blue = 	b <= 1.0 ? (int) Math.floor(b * 255) : 255;
        int pixelColor = getColorInteger(c_red,c_green,c_blue,c_alpha);
        return pixelColor;
	}
    //Do NOT modify this function    
    public void resetImage(){
    	for (int j = 0; j < image.getHeight(); j++) {
	        for (int i = 0; i < image.getWidth(); i++) {
	            image.setRGB(i, j, 0);
	        }
	    }
    }
    //Do NOT modify this function
    void getLightVector(double[] lightVector, double[] halfVector, double[] viewVec){
    	VectorMath.setVector(lightVector, viewVec[0], viewVec[1], viewVec[2]);
    	for (int i=0; i<3; i++) {
            halfVector[i] = viewVec[i] + lightVector[i];
        }
        double l = VectorMath.length(halfVector);
        for (int i=0; i<3; i++) {
            halfVector[i] /= l;
        }
    }
   
    //used by the slicer
    //Do NOT modify this function
    void getViewPlaneVectors(double[] viewMatrix, double viewVec[], double uVec[], double vVec[]) {
            VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
	    VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
	    VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);
	}
    
    //used by the slicer	
    //Do NOT modify this function
	void computeVolumeCenter(double volumeCenter[]) {
		VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);
	}
	
    //used by the slicer
    //Do NOT modify this function
	void computePixelCoordinates(double pixelCoord[], double volumeCenter[], double uVec[], double vVec[], int i, int j) {
            // Coordinates of a plane centered at the center of the volume (volumeCenter and oriented according to the plane defined by uVec and vVec
		int imageCenter = image.getWidth()/2;
		pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0];
                pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1];
                pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2];
	}
    //Do NOT modify this function
	void computePixelCoordinatesBehind(double pixelCoord[], double viewVec[], double uVec[], double vVec[], int i, int j) {
		int imageCenter = image.getWidth()/2;
                // Pixel coordinate is calculate having the center (0,0) of the view plane aligned with the center of the volume and moved a distance equivalent
                // to the diaganal to make sure I am far away enough.
                double diagonal = Math.sqrt((volume.getDimX()*volume.getDimX())+(volume.getDimY()*volume.getDimY())+ (volume.getDimZ()*volume.getDimZ()))/2;               
		pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + viewVec[0] * diagonal + volume.getDimX() / 2.0;
                pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + viewVec[1] * diagonal + volume.getDimY() / 2.0;
                pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + viewVec[2] * diagonal + volume.getDimZ() / 2.0;
	}
	
    //Do NOT modify this function
    public int getColorInteger(int c_red, int c_green, int c_blue, int c_alpha) {
    	int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
    	return pixelColor;
    } 
    //Do NOT modify this function
    public RaycastRenderer() {
        panel = new RaycastRendererPanel(this);
        panel.setSpeedLabel("0");
    }
    //Do NOT modify this function
    public void setVolume(Volume vol) {
        System.out.println("Assigning volume");
        volume = vol;

        System.out.println("Computing gradients");
        gradients = new GradientVolume(vol);

        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY()
                + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }
        image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
       
        // Initialize transferfunction 
        tFunc = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        tFunc.setTestFunc();
        tFunc.addTFChangeListener(this);
        tfEditor = new TransferFunctionEditor(tFunc, volume.getHistogram());
        
        tFunc2D= new TransferFunction2D((short) (volume.getMaximum() / 2), 0.2);
        tfEditor2D = new TransferFunction2DEditor(tFunc2D,volume, gradients);
        tfEditor2D.addTFChangeListener(this);

        System.out.println("Finished initialization of RaycastRenderer");
    }
    //Do NOT modify this function
    public RaycastRendererPanel getPanel() {
        return panel;
    }

    //Do NOT modify this function
    public TransferFunction2DEditor getTF2DPanel() {
        return tfEditor2D;
    }
    //Do NOT modify this function
    public TransferFunctionEditor getTFPanel() {
        return tfEditor;
    }
    //Do NOT modify this function
    public void setShadingMode(boolean mode) {
        shadingMode = mode;
        changed();
    }
    //Do NOT modify this function
    public void setMIPMode() {
        setMode(false, true, false, false);
    }
    //Do NOT modify this function
    public void setSlicerMode() {
        setMode(true, false, false, false);
    }
    //Do NOT modify this function
    public void setCompositingMode() {
        setMode(false, false, true, false);
    }
    //Do NOT modify this function
    public void setTF2DMode() {
        setMode(false, false, false, true);
    }
    //Do NOT modify this function
    private void setMode(boolean slicer, boolean mip, boolean composite, boolean tf2d) {
        slicerMode = slicer;
        mipMode = mip;
        compositingMode = composite;
        tf2dMode = tf2d;        
        changed();
    }
    //Do NOT modify this function
    private boolean intersectLinePlane(double[] plane_pos, double[] plane_normal,
            double[] line_pos, double[] line_dir, double[] intersection) {

        double[] tmp = new double[3];

        for (int i = 0; i < 3; i++) {
            tmp[i] = plane_pos[i] - line_pos[i];
        }

        double denom = VectorMath.dotproduct(line_dir, plane_normal);
        if (Math.abs(denom) < 1.0e-8) {
            return false;
        }

        double t = VectorMath.dotproduct(tmp, plane_normal) / denom;

        for (int i = 0; i < 3; i++) {
            intersection[i] = line_pos[i] + t * line_dir[i];
        }

        return true;
    }
    //Do NOT modify this function
    private boolean validIntersection(double[] intersection, double xb, double xe, double yb,
            double ye, double zb, double ze) {

        return (((xb - 0.5) <= intersection[0]) && (intersection[0] <= (xe + 0.5))
                && ((yb - 0.5) <= intersection[1]) && (intersection[1] <= (ye + 0.5))
                && ((zb - 0.5) <= intersection[2]) && (intersection[2] <= (ze + 0.5)));

    }
    //Do NOT modify this function
    private void intersectFace(double[] plane_pos, double[] plane_normal,
            double[] line_pos, double[] line_dir, double[] intersection,
            double[] entryPoint, double[] exitPoint) {

        boolean intersect = intersectLinePlane(plane_pos, plane_normal, line_pos, line_dir,
                intersection);
        if (intersect) {

            double xpos0 = 0;
            double xpos1 = volume.getDimX();
            double ypos0 = 0;
            double ypos1 = volume.getDimY();
            double zpos0 = 0;
            double zpos1 = volume.getDimZ();

            if (validIntersection(intersection, xpos0, xpos1, ypos0, ypos1,
                    zpos0, zpos1)) {
                if (VectorMath.dotproduct(line_dir, plane_normal) < 0) {
                    entryPoint[0] = intersection[0];
                    entryPoint[1] = intersection[1];
                    entryPoint[2] = intersection[2];
                } else {
                    exitPoint[0] = intersection[0];
                    exitPoint[1] = intersection[1];
                    exitPoint[2] = intersection[2];
                }
            }
        }
    }
 
  
   
    
    //Do NOT modify this function
    void computeEntryAndExit(double[] p, double[] viewVec, double[] entryPoint, double[] exitPoint) {

        for (int i = 0; i < 3; i++) {
            entryPoint[i] = -1;
            exitPoint[i] = -1;
        }

        double[] plane_pos = new double[3];
        double[] plane_normal = new double[3];
        double[] intersection = new double[3];

        VectorMath.setVector(plane_pos, volume.getDimX(), 0, 0);
        VectorMath.setVector(plane_normal, 1, 0, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, -1, 0, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, volume.getDimY(), 0);
        VectorMath.setVector(plane_normal, 0, 1, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, 0, -1, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, volume.getDimZ());
        VectorMath.setVector(plane_normal, 0, 0, 1);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, 0, 0, -1);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

    }

    //Do NOT modify this function
    private void drawBoundingBox(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();

    }
    //Do NOT modify this function
    @Override
    public void visualize(GL2 gl) {

        double[] viewMatrix = new double[4 * 4];
        
        if (volume == null) {
            return;
        }
        	
         drawBoundingBox(gl);

        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, viewMatrix, 0);

        long startTime = System.currentTimeMillis();
        if (slicerMode) {
            slicer(viewMatrix);    
        } else {
            raycast(viewMatrix);
        }
        
        long endTime = System.currentTimeMillis();
        double runningTime = (endTime - startTime);
        panel.setSpeedLabel(Double.toString(runningTime));

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // draw rendered image as a billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        gl.glPopAttrib();


        if (gl.glGetError() > 0) {
            System.out.println("some OpenGL error: " + gl.glGetError());
        }

    }
    private BufferedImage image;

    //Do NOT modify this function
    @Override
    public void changed() {
        for (int i=0; i < listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }
}
