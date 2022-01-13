package RNA_Scope_Utils;



import StardistOrion.StarDist2D;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.GaussianBlur3D;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom.Point3D;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageLabeller;
import mcib3d.image3d.distanceMap3d.EDT;
import mcib3d.image3d.processing.FastFilters3D;
import mcib3d.image3d.regionGrowing.Watershed3D;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



/**
 *
 * @author phm
 */

public class RNA_Scope_Processing {
    
    public CLIJ2 clij2 = CLIJ2.getInstance();
    public double minDots = 0.5;
    public double maxDots = 5;
    public Object syncObject = new Object();
    public final double stardistPercentileBottom = 0.2;
    public final double stardistPercentileTop = 99.8;
    public final double stardistProbThreshNuc = 0.55;
    public final double stardistOverlayThreshNuc = 0.4;
    public final double stardistProbThreshDots = 0.7;
    public final double stardistOverlayThreshDots = 0.45;
    public File modelsPath = new File(IJ.getDirectory("imagej")+File.separator+"models");
    public String stardistModelNucleus = "";
    public String stardistModelGenes = "";
    public String stardistOutput = "Label Image"; 
    private Calibration cal;
    

    
    private RNA_Scope.RNA_Scope_Main main = new RNA_Scope.RNA_Scope_Main();
     
    
    /*
    Find starDist models in Fiji models folder
    */
    public String[] findStardistModels() {
        FilenameFilter filter = (dir, name) -> name.endsWith(".zip");
        File[] modelList = modelsPath.listFiles(filter);
        String[] models = new String[modelList.length];
        for (int i = 0; i < modelList.length; i++) {
            models[i] = modelList[i].getName();
        }
        Arrays.sort(models);
        return(models);
    }
    
    
     /**
     * check  installed modules
     * @return 
     */
    public boolean checkInstalledModules() {
        // check install
        ClassLoader loader = IJ.getClassLoader();
        try {
            loader.loadClass("net.haesleinhuepf.clij2.CLIJ2");
        } catch (ClassNotFoundException e) {
            IJ.log("CLIJ not installed, please install from update site");
            return false;
        }
        try {
            loader.loadClass("mcib3d.geom.Object3D");
        } catch (ClassNotFoundException e) {
            IJ.log("3D ImageJ Suite not installed, please install from update site");
            return false;
        }
        return true;
    }
    
    
    /**
     *
     * @param img
     */
    public void closeImages(ImagePlus img) {
        img.flush();
        img.close();
    }

    /**
     * Clear out side roi
     * @param img
     * @param roi
     */
    public void clearOutSide(ImagePlus img, Roi roi) {
        for (int n = 1; n <= img.getNSlices(); n++) {
            ImageProcessor ip = img.getImageStack().getProcessor(n);
            ip.setRoi(roi);
            ip.setBackgroundValue(0);
            ip.setColor(0);
            ip.fillOutside(roi);
        }
        img.updateAndDraw();
    }
    
/**
     * return objects population in an binary image
     * @param img
     * @return pop objects population
     */

    public  Objects3DPopulation getPopFromImage(ImagePlus img) {
        // label binary images first
        ImageLabeller labeller = new ImageLabeller();
        ImageInt labels = labeller.getLabels(ImageHandler.wrap(img));
        Objects3DPopulation pop = new Objects3DPopulation(labels);
        return pop;
    } 
    
  /**
     * return objects population in an binary image
     * Using CLIJ2
     * @param imgCL
     * @return pop
     */

    private Objects3DPopulation getPopFromClearBuffer(ClearCLBuffer imgCL, Roi roi) {
        ClearCLBuffer output = clij2.create(imgCL);
        clij2.connectedComponentsLabelingBox(imgCL, output);
        ImagePlus imgLab  = clij2.pull(output);
        imgLab.setCalibration(cal);
        if (roi != null) {
            roi.setLocation(0, 0);
            clearOutSide(imgLab, roi);
        }   
        Objects3DPopulation pop = new Objects3DPopulation(imgLab);
        Objects3DPopulation popFil = new Objects3DPopulation(pop.getObjectsWithinVolume(minDots, maxDots, true));
        pop = null;
        clij2.release(output);
        return (popFil);
    }  
    
    
    /**
     * gaussian 3D filter 
     * Using CLIJ2
     * @param imgCL
     * @param sizeX
     * @param sizeY
     * @param sizeZ
     * @return imgOut
     */
 
    public ClearCLBuffer gaussianBlur3D(ClearCLBuffer imgCL, double sizeX, double sizeY, double sizeZ) {
        ClearCLBuffer imgOut = clij2.create(imgCL);
        clij2.gaussianBlur3D(imgCL, imgOut, sizeX, sizeY, sizeZ);
        clij2.release(imgCL);
        return(imgOut);
    }
    
     /**  
     * median 3D box filter
     * Using CLIJ2
     * @param imgCL
     * @param sizeX
     * @param sizeY
     * @param sizeZ
     * @return imgOut
     */ 
    public ClearCLBuffer medianFilter(ClearCLBuffer imgCL, double sizeX, double sizeY, double sizeZ) {
        ClearCLBuffer imgIn = clij2.push(imgCL);
        ClearCLBuffer imgOut = clij2.create(imgIn);
        clij2.median3DBox(imgIn, imgOut, sizeX, sizeY, sizeZ);
        clij2.release(imgCL);
        return(imgOut);
    }
    
    /**
     * Difference of Gaussians 
     * Using CLIJ2
     * @param imgCL
     * @param sizeX1
     * @param sizeY1
     * @param sizeZ1
     * @param sizeX2
     * @param sizeY2
     * @param sizeZ2
     * @return imgGauss
     */ 
    public ClearCLBuffer DOG(ClearCLBuffer imgCL, double sizeX1, double sizeY1, double sizeZ1, double sizeX2, double sizeY2, double sizeZ2) {
        ClearCLBuffer imgCLDOG = clij2.create(imgCL);
        clij2.differenceOfGaussian3D(imgCL, imgCLDOG, sizeX1, sizeY1, sizeZ1, sizeX2, sizeY2, sizeZ2);
        clij2.release(imgCL);
        return(imgCLDOG);
    }
    
    /**
     * Fill hole
     * USING CLIJ2
     */
    private void fillHole(ClearCLBuffer imgCL) {
        long[] dims = clij2.getDimensions(imgCL);
        ClearCLBuffer slice = clij2.create(dims[0], dims[1]);
        ClearCLBuffer slice_filled = clij2.create(slice);
        for (int z = 0; z < dims[2]; z++) {
            clij2.copySlice(imgCL, slice, z);
            clij2.binaryFillHoles(slice, slice_filled);
            clij2.copySlice(slice_filled, imgCL, z);
        }
        clij2.release(slice);
        clij2.release(slice_filled);
    }
    
  /**
   * Open
   * USING CLIJ2
   * @param imgCL
   * @return imgCLOut
   */
    private ClearCLBuffer open(ClearCLBuffer imgCL) {
        ClearCLBuffer imgCLOut = clij2.create(imgCL);
        clij2.openingBox(imgCL, imgCLOut, 1);
        clij2.release(imgCL);
        return(imgCLOut);
    }
    
    /**
     * Threshold 
     * USING CLIJ2
     * @param imgCL
     * @param thMed
     * @param fill 
     */
    public ClearCLBuffer threshold(ClearCLBuffer imgCL, String thMed, boolean fill) {
        ClearCLBuffer imgCLBin = clij2.create(imgCL);
        clij2.automaticThreshold(imgCL, imgCLBin, thMed);
        if (fill)
            fillHole(imgCLBin);
        return(imgCLBin);
    }
    

    /**
     * Find gene population
     * @param imgGeneRef
     * @return genePop
     */
    public Objects3DPopulation findGenePop(ImagePlus imgGeneRef, Roi roi) {
        cal = imgGeneRef.getCalibration();
        ImagePlus img = new Duplicator().run(imgGeneRef);
        ClearCLBuffer imgCL = clij2.push(img);
        ClearCLBuffer imgCLMed = medianFilter(imgCL, 1, 1, 1);
        clij2.release(imgCL);
        ClearCLBuffer imgCLDOG = DOG(imgCLMed, main.DOGMin, main.DOGMin, main.DOGMin, main.DOGMax, main.DOGMax, main.DOGMax);
        clij2.release(imgCLMed);
        ClearCLBuffer imgCLBin = threshold(imgCLDOG, main.geneThMethod, false); 
        clij2.release(imgCLDOG);
        Objects3DPopulation genePop = getPopFromClearBuffer(imgCLBin, roi);
        clij2.release(imgCLBin);       
        return(genePop);
    }
    
    
    /**
     * labelled color nucleus population
     */
    public ImagePlus colorPop (Objects3DPopulation cellsPop,  ImagePlus img, boolean number) {
        //create image objects population
        Font tagFont = new Font("SansSerif", Font.PLAIN, 30);
        ImageHandler imgObj = ImageInt.wrap(img).createSameDimensions();
        imgObj.setCalibration(cal);
        for (int i = 0; i < cellsPop.getNbObjects(); i++) {
            Object3D obj = cellsPop.getObject(i);
            obj.draw(imgObj, (i+1));
            if (number) {
                String name = Integer.toString(i+1);
                int[] box = obj.getBoundingBox();
                int z = (int)obj.getCenterZ();
                int x = box[0] - 1;
                int y = box[2] - 1;
                imgObj.getImagePlus().setSlice(z+1);
                ImageProcessor ip = imgObj.getImagePlus().getProcessor();
                ip.setFont(tagFont);
                ip.setColor((i+1));
                ip.drawString(name, x, y);
                imgObj.getImagePlus().updateAndDraw();
            }
        } 
        return(imgObj.getImagePlus());
    } 
    
    /**
     * Tags cell with gene spot, Integrated intensity and max spot Integrated intensty ....
     * @param cellsPop (nucleus dilated population)
     * @param dotsPop gene population
     * @param imgGeneRef
     * @param imgGeneX
     */
    
    public ArrayList<Cell> tagsCells(Objects3DPopulation cellsPop, Objects3DPopulation dotsRefPop, Objects3DPopulation dotsXPop, ImagePlus imgGeneRef,
            ImagePlus imgGeneX, Roi roiBgGeneRef, Roi roiBgGeneX) {
        
        IJ.showStatus("Finding cells with gene reference ...");
        ArrayList<Cell> cells = new ArrayList<>();
        ImageHandler imhRef = ImageHandler.wrap(imgGeneRef);
        ImageHandler imhX = ImageHandler.wrap(imgGeneX);
        int index = 0;
        
        ImagePlus imgGeneRefCrop = new ImagePlus();
        ImagePlus imgGeneXCrop = new ImagePlus();
        double bgGeneRef = 0, bgGeneX = 0;
        
        // crop image for background
        if (roiBgGeneRef != null) {
            imgGeneRef.setRoi(roiBgGeneRef);
            imgGeneRefCrop = imgGeneRef.crop("stack");
            imgGeneX.setRoi(roiBgGeneX);
            imgGeneXCrop = imgGeneX.crop("stack");
        }
       
        
        for (int i = 0; i < cellsPop.getNbObjects(); i++) {
            double geneRefDotsVol = 0, geneXDotsVol = 0;
            double geneRefDotsInt = 0, geneXDotsInt = 0;
            
            // calculate cell parameters
            index++;
            Object3D cellObj = cellsPop.getObject(i);
            double zCell = cellObj.getCenterZ();
            double cellVol = cellObj.getVolumePixels();
            double cellGeneRefInt = cellObj.getIntegratedDensity(imhRef);
            double cellGeneXInt = cellObj.getIntegratedDensity(imhX);
            
            int cellMinZ = cellObj.getZmin() == 0 ? 1 : cellObj.getZmin();
            int cellMaxZ = cellObj.getZmax() > imgGeneRef.getNSlices() ? imgGeneRef.getNSlices() : cellObj.getZmax();
            
            
            // Cell background
            if (roiBgGeneRef != null) {
                bgGeneRef = find_background(imgGeneRefCrop, cellMinZ, cellMaxZ);
                bgGeneX = find_background(imgGeneXCrop, cellMinZ, cellMaxZ);
            }
            else {
                bgGeneRef = main.calibBgGeneRef;
                bgGeneX = main.calibBgGeneX;
            } 
            //System.out.println("Mean Background  ref = " + bgGeneRef + " zmin "+cellMinZ+" zmax "+cellMaxZ);
            //System.out.println("Mean Background  X = " + bgGeneX);
            
            // ref dots parameters
            for (int n = 0; n < dotsRefPop.getNbObjects(); n++) {
                Object3D dotObj = dotsRefPop.getObject(n);
                // find dots inside cell
                if (dotObj.hasOneVoxelColoc(cellObj)) {
                    geneRefDotsVol += dotObj.getVolumePixels();
                    geneRefDotsInt += dotObj.getIntegratedDensity(imhRef);
                }
            }
            
            // X dots parameters
            for (int n = 0; n < dotsXPop.getNbObjects(); n++) {
                Object3D dotObj = dotsXPop.getObject(n);
                // find dots inside cell
                if (dotObj.hasOneVoxelColoc(cellObj)) {
                    geneXDotsVol += dotObj.getVolumePixels();
                    geneXDotsInt += dotObj.getIntegratedDensity(imhX);
                }
            }
            // dots number based on cell intensity
            int nbGeneRefDotsCellInt = Math.round((float)((cellGeneRefInt - bgGeneRef * cellVol) / main.singleDotIntGeneRef));
            int nbGeneXDotsCellInt = Math.round((float)((cellGeneXInt - bgGeneX * cellVol) / main.singleDotIntGeneX));
            
            // dots number based on dots segmented intensity
            int nbGeneRefDotsSegInt = Math.round((float)((geneRefDotsInt - bgGeneRef * geneRefDotsVol) / main.singleDotIntGeneRef));
            int nbGeneXDotsSegInt = Math.round((float)((geneXDotsInt - bgGeneX * geneXDotsVol) / main.singleDotIntGeneX));
            
            Cell cell = new Cell(index, cellVol, zCell, cellGeneRefInt, bgGeneRef, geneRefDotsVol, geneRefDotsInt, nbGeneRefDotsCellInt, nbGeneRefDotsSegInt, cellGeneXInt,
                    bgGeneX, geneXDotsVol, geneXDotsInt, nbGeneXDotsCellInt, nbGeneXDotsSegInt);
            cells.add(cell);
        }
        closeImages(imgGeneRefCrop);
        closeImages(imgGeneXCrop);
        return(cells);
    }
    
    /**
     * Return dilated object restriced to image borders
     * @param img
     * @param obj
     * @return 
     */
    private Object3DVoxels dilCellObj(ImagePlus img, Object3D obj) {
        Object3D objDil = obj.getDilatedObject((float)(main.nucDil/cal.pixelWidth), (float)(main.nucDil/cal.pixelHeight), 
                (float)(main.nucDil/cal.pixelDepth));
        // check if object go outside image
        if (objDil.getXmin() < 0 || objDil.getXmax() > img.getWidth() || objDil.getYmin() < 0 || objDil.getYmax() > img.getHeight()
                || objDil.getZmin() < 0 || objDil.getZmax() > img.getNSlices()) {
            Object3DVoxels voxObj = new Object3DVoxels(objDil.listVoxels(ImageHandler.wrap(img)));
            return(voxObj);
        }
        else
            return(objDil.getObject3DVoxels());
    }
    
    
    public  Objects3DPopulation findNucleus(ImagePlus imgNuc) {
        Objects3DPopulation nucPopOrg = new Objects3DPopulation();
        cal = imgNuc.getCalibration();
        nucPopOrg = find_nucleus2(imgNuc);
        System.out.println("-- Total nucleus Population :"+nucPopOrg.getNbObjects());
        // size filter
        Objects3DPopulation nucPop = new Objects3DPopulation(nucPopOrg.getObjectsWithinVolume(main.minNucVol, main.maxNucVol, true));
        int nbNucPop = nucPop.getNbObjects();
        System.out.println("-- Total nucleus Population after size filter: "+ nbNucPop);
        // create dilated nucleus population
        Objects3DPopulation cellsPop = new Objects3DPopulation();
        for (int o = 0; o < nucPop.getNbObjects(); o++) {
            Object3D obj = nucPop.getObject(o);
            cellsPop.addObject(dilCellObj(imgNuc, obj));
        }
        return(cellsPop);
    }
    
    
    
    /**
     * Nucleus segmentation 2
     * @param imgNuc
     * @return cellPop
     */
    public Objects3DPopulation find_nucleus2(ImagePlus imgNuc) {
        ImagePlus img = new Duplicator().run(imgNuc);
        ImageStack stack = new ImageStack(img.getWidth(), imgNuc.getHeight());
        for (int i = 1; i <= img.getStackSize(); i++) {
            IJ.showStatus("Finding nucleus section "+i+" / "+img.getStackSize());
            img.setZ(i);
            img.updateAndDraw();
            IJ.run(img, "Nuclei Outline", "blur=20 blur2=30 threshold_method="+main.nucThMethod+" outlier_radius=50 outlier_threshold=1 max_nucleus_size=100 "
                    + "min_nucleus_size=10 erosion=5 expansion_inner=5 expansion=5 results_overlay");
            img.setZ(1);
            img.updateAndDraw();
            ImagePlus mask = new ImagePlus("mask", img.createRoiMask().getBufferedImage());
            ImageProcessor ip =  mask.getProcessor();
            ip.invertLut();
            for (int n = 0; n < 3; n++) 
                ip.erode();
            stack.addSlice(ip);
        }
        ImagePlus imgStack = new ImagePlus("Nucleus", stack);        
        IJ.showStatus("Starting watershed...");
        ImagePlus imgWater = WatershedSplit(imgStack, 8);
        closeImages(imgStack);
        imgWater.setCalibration(cal);
        Objects3DPopulation cellPop = new Objects3DPopulation(imgWater);
        cellPop.removeObjectsTouchingBorders(imgWater, false);
        closeImages(imgWater);
        closeImages(img);
        return(cellPop);
    }
    
    /**
     * Find gene population with Stardist
     */
    public Objects3DPopulation stardistGenePop(ImagePlus imgGene, Roi roi) throws IOException{
        roi.setLocation(0, 0);
        imgGene.setRoi(roi);
        cal = imgGene.getCalibration();
        ImagePlus img = new Duplicator().run(imgGene);
        ClearCLBuffer imgCL = clij2.push(img);
        ClearCLBuffer imgCLMed = medianFilter(imgCL, 1, 1, 1);
        clij2.release(imgCL);
        ImagePlus imgGeneMed = clij2.pull(imgCLMed);
        clij2.release(imgCLMed);
        imgGeneMed.setCalibration(cal);
        
        // Go StarDist
        File starDistModelFile = new File(stardistModelGenes);
        StarDist2D star = new StarDist2D(syncObject, starDistModelFile);
        star.loadInput(imgGeneMed);
        star.setParams(stardistPercentileBottom, stardistPercentileTop, stardistProbThreshDots, stardistOverlayThreshDots, stardistOutput);
        star.run();
        // label in 3D
        ImagePlus imgGeneLab = star.associateLabels();
        imgGeneLab.setCalibration(cal);
        if (roi != null) {
            roi.setLocation(0, 0);
            clearOutSide(imgGeneLab, roi);
        }
        Objects3DPopulation genePop = new Objects3DPopulation(getPopFromImage(imgGeneLab).getObjectsWithinVolume(minDots, maxDots, true));
        System.out.println(genePop.getNbObjects()+" dots");
        closeImages(imgGeneLab);
        closeImages(imgGeneMed);
        return(genePop);
        }
    
    
/** Look for all nuclei
         Do z slice by slice stardist 
         * return nuclei population
         */
        public Objects3DPopulation stardistNucleiPop(ImagePlus imgNuc) throws IOException{
            cal = imgNuc.getCalibration();
            IJ.run(imgNuc, "Remove Outliers", "block_radius_x=40 block_radius_y=40 standard_deviations=1 stack");
            // Clear unfocus Z plan
            Find_focused_slices focus = new Find_focused_slices();
            focus.run(imgNuc);
            // Go StarDist
            File starDistModelFile = new File(stardistModelNucleus);
            StarDist2D star = new StarDist2D(syncObject, starDistModelFile);
            star.loadInput(imgNuc);
            star.setParams(stardistPercentileBottom, stardistPercentileTop, stardistProbThreshNuc, stardistOverlayThreshNuc, stardistOutput);
            star.run();
            // label in 3D
            ImagePlus nuclei = star.associateLabels();
            nuclei.setCalibration(cal);
            ImageInt label3D = ImageInt.wrap(nuclei);
            label3D.setCalibration(cal);
            Objects3DPopulation nucPop = new Objects3DPopulation(label3D);
            Objects3DPopulation nPop = new Objects3DPopulation(nucPop.getObjectsWithinVolume(main.minNucVol, main.maxNucVol, true));
            closeImages(nuclei);
            return(nPop);
        }
    
    private ImagePlus WatershedSplit(ImagePlus binaryMask, float rad) {
        float resXY = 1;
        float resZ = 1;
        float radXY = rad;
        float radZ = rad;
        Calibration cal = binaryMask.getCalibration();
        if (cal != null) {
            resXY = (float) cal.pixelWidth;
            resZ = (float) cal.pixelDepth;
            radZ = radXY * (resXY / resZ);
        }
        ImageInt imgMask = ImageInt.wrap(binaryMask);
        ImageFloat edt = EDT.run(imgMask, 0, resXY, resZ, false, 0);
        ImageHandler edt16 = edt.convertToShort(true);
        ImagePlus edt16Plus = edt16.getImagePlus();
        GaussianBlur3D.blur(edt16Plus, 2.0, 2.0, 2.0);
        edt16 = ImageInt.wrap(edt16Plus);
        edt16.intersectMask(imgMask);
        // seeds
        ImageHandler seedsImg = FastFilters3D.filterImage(edt16, FastFilters3D.MAXLOCAL, radXY, radXY, radZ, 0, false);
        Watershed3D water = new Watershed3D(edt16, seedsImg, 0, 0);
        water.setLabelSeeds(true);
        return(water.getWatershedImage3D().getImagePlus());
    }
    

    
    /**
     * Find min background roi
     * @param img
     * @param size
     * @return 
     */
    public Roi findRoiBackgroundAuto(ImagePlus img, double bgGene) {
        // scroll gene image and measure bg intensity in roi 
        // take roi at intensity nearest from bgGene
        
        ArrayList<RoiBg> intBgFound = new ArrayList<RoiBg>();
        
        for (int x = 0; x < img.getWidth() - main.roiBgSize; x += main.roiBgSize) {
            for (int y = 0; y < img.getHeight() - main.roiBgSize; y += main.roiBgSize) {
                Roi roi = new Roi(x, y, main.roiBgSize, main.roiBgSize);
                img.setRoi(roi);
                ImagePlus imgCrop = img.crop("stack");
                double bg = find_background(imgCrop, 1, img.getNSlices());
                intBgFound.add(new RoiBg(roi, bg));
                closeImages(imgCrop);
            }
        }
        img.deleteRoi();
        // sort RoiBg on bg value
        intBgFound.sort(Comparator.comparing(RoiBg::getBgInt));
        
        // Find nearest value from bgGene
        double min = Double.MAX_VALUE;
        double closest = bgGene;
        Roi roiBg = null;
        for (RoiBg v : intBgFound) {
            final double diff = Math.abs(v.getBgInt() - bgGene);
            if (diff < min) {
                min = diff;
                closest = v.getBgInt();
                roiBg = v.getRoi();
            }
        }
        int roiCenterX = roiBg.getBounds().x+(main.roiBgSize/2);
        int roiCenterY = roiBg.getBounds().y+(main.roiBgSize/2);
        System.out.println("Roi auto background found = "+closest+" center x = "+roiCenterX+", y = "+roiCenterY);
        return(roiBg);
    }
    
    
    /*
    * Get Mean of intensity in stack
    */
    public double find_background(ImagePlus img, int zMin, int zMax) {
        ImagePlus imgProj = new Duplicator().run(img, zMin, zMax);
        ResultsTable rt = new ResultsTable();
        Analyzer ana = new Analyzer(imgProj, Measurements.MEAN, rt);
        double meanBg = 0;
        ZProjector zProj = new ZProjector(imgProj);
        zProj.setMethod(ZProjector.AVG_METHOD);
        zProj.doProjection();
        ana.measure();
        meanBg = rt.getValue("Mean", 0);
        rt.reset();
        return(meanBg);  
    }
    
    
    /**
     * Label object
     * @param popObj
     * @param img 
     */
    public void labelsObject (Objects3DPopulation popObj, ImagePlus img, int fontSize) {
        Font tagFont = new Font("SansSerif", Font.PLAIN, fontSize);
        String name;
        for (int n = 0; n < popObj.getNbObjects(); n++) {
            Object3D obj = popObj.getObject(n);
            name = Integer.toString(n+1);
            int[] box = obj.getBoundingBox();
            int z = (int)obj.getCenterZ();
            int x = box[0] - 1;
            int y = box[2] - 1;
            img.setSlice(z+1);
            ImageProcessor ip = img.getProcessor();
            ip.setFont(tagFont);
            ip.setColor(255);
            ip.drawString(name, x, y);
            img.updateAndDraw();
        }
    }
    
    /**
     * 
     * @param xmlFile
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public ArrayList<Point3D> readXML(String xmlFile) throws ParserConfigurationException, SAXException, IOException {
        ArrayList<Point3D> ptList = new ArrayList<>();
        double x = 0, y = 0 ,z = 0;
        File fXmlFile = new File(xmlFile);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("Marker");
        for (int n = 0; n < nList.getLength(); n++) {
            Node nNode = nList.item(n);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                x = Double.parseDouble(eElement.getElementsByTagName("MarkerX").item(0).getTextContent());
                y = Double.parseDouble(eElement.getElementsByTagName("MarkerY").item(0).getTextContent());
                z = Double.parseDouble(eElement.getElementsByTagName("MarkerZ").item(0).getTextContent());
            }
            Point3D pt = new Point3D(x, y, z);
            ptList.add(pt);
        }
        return(ptList);
    }
    
    public void InitResults(String outDirResults) throws IOException {
        // initialize results files
        // Detailed results
        FileWriter  fwAnalyze_detail = new FileWriter(outDirResults + main.autoBackground +"_results.xls",false);
        main.output_detail_Analyze = new BufferedWriter(fwAnalyze_detail);
        // write results headers
        main.output_detail_Analyze.write("Image Name\t#Cell\tCell Vol (pixel3)\tCell Z center\tCell Integrated intensity in gene ref. channel\tMean background intensity in ref. channel\t"
                + "Total dots gene ref. (based on cell intensity)\tDots ref. volume (pixel3)\tIntegrated intensity of dots ref. channel\t"
                + "Total dots gene ref (based on dots seg intensity)\tCell Integrated intensity in gene X channel\tMean background intensity in X channel\t"
                + "Total dots gene X (based on cell intensity)\tDots X volume (pixel3)\tIntegrated intensity of dots X channel\tTotal dots gene X (based on dots seg intensity)\n");
        main.output_detail_Analyze.flush();
    }
    
    /**
     * Save nucleus with labbelled colors
     * @param imgNuc
     * @param cellsPop
     * @param outDirResults
     * @param rootName
     */
    public void saveCells (ImagePlus imgNuc, Objects3DPopulation cellsPop, String outDirResults, String rootName) {
        ImagePlus imgColorPop = colorPop (cellsPop, imgNuc, main.nucNumber);
        IJ.run(imgColorPop, "3-3-2 RGB", "");
        FileSaver ImgColorObjectsFile = new FileSaver(imgColorPop);
        ImgColorObjectsFile.saveAsTiff(outDirResults + rootName + "_Cells-ColorObjects.tif");
        closeImages(imgColorPop);
    }

    
    /**
     * save images objects population
     * @param imgNuc
     * @param cellsPop
     * @param geneRefPop
     * @param geneXPop
     * @param outDirResults
     * @param rootName
     */
    public void saveDotsImage (ImagePlus imgNuc, Objects3DPopulation cellsPop, Objects3DPopulation geneRefPop, Objects3DPopulation geneXPop,
            String outDirResults, String rootName) {
        // red dots geneRef , dots green geneX, blue nucDilpop
        ImageHandler imgCells = ImageHandler.wrap(imgNuc).createSameDimensions();
        ImageHandler imgDotsGeneRef = ImageHandler.wrap(imgNuc).createSameDimensions();
        ImageHandler imgDotsGeneX = ImageHandler.wrap(imgNuc).createSameDimensions();
        ImageHandler imgCellNumbers = ImageHandler.wrap(imgNuc).createSameDimensions();
        // draw nucleus dots population
        cellsPop.draw(imgCells, 255);
        labelsObject(cellsPop, imgCellNumbers.getImagePlus(), 24);
        geneRefPop.draw(imgDotsGeneRef, 255);
        geneXPop.draw(imgDotsGeneX, 255);
        ImagePlus[] imgColors = {imgDotsGeneRef.getImagePlus(), imgDotsGeneX.getImagePlus(), imgCells.getImagePlus(), null, imgCellNumbers.getImagePlus()};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgObjects.setCalibration(cal);
        IJ.run(imgObjects, "Enhance Contrast", "saturated=0.35");

        // Save images
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(outDirResults + rootName + "_DotsObjects.tif");
        imgCells.closeImagePlus();
        imgDotsGeneRef.closeImagePlus();
        imgDotsGeneX.closeImagePlus();
    }
}
