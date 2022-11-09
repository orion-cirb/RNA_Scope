package RNA_Scope_Utils;



import StardistOrion.StarDist2D;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom.Point3DInt;
import mcib3d.geom2.BoundingBox;
import mcib3d.geom2.Object3DComputation;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Object3DPlane;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.Objects3DIntPopulationComputation;
import mcib3d.geom2.VoxelInt;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.geom2.measurementsPopulation.MeasurePopulationColocalisation;
import mcib3d.geom2.measurementsPopulation.PairObjects3DInt;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



/**
 *
 * @author phm
 */

public class RNA_Scope_Tools {
    
    public CLIJ2 clij2 = CLIJ2.getInstance();
    public double singleDotIntGeneRef = 0, singleDotIntGeneX = 0, singleDotIntGeneY = 0;
    public double calibBgGeneRef = 0, calibBgGeneX = 0, calibBgGeneY = 0;
    public double roiBgSize = 100;
    public String stardistModelNucleus = "StandardFluo.zip";
    public String stardistModelGenes = "pmls2.zip";
    
    public Object syncObject = new Object();
    public final double stardistPercentileBottom = 0.2;
    public final double stardistPercentileTop = 99.8;
    public final double stardistProbThreshNuc = 0.70;
    public final double stardistOverlayThreshNuc = 0.4;
    public final double stardistProbThreshDots = 0.2;
    public final double stardistOverlayThreshDots = 0.45;
    public File modelsPath = new File(IJ.getDirectory("imagej")+File.separator+"models");
    public String stardistOutput = "Label Image"; 
    public Calibration cal = new Calibration();
    private float pixVol = 0;
    
    public String genesDetection = "DOG";
    private double DOGMin = 2;
    private double DOGMax = 4;
    
    private double minNucVol = 250;
    private double maxNucVol = 8000;
    private double minGeneVol = 5;
    private double maxGeneVol = 800;
    public String bgDetection = "";
    
    public String[] channelsName = {"DAPI", "Gene Ref", "Gene X", "Gene Y"};  
    private int channelsSize = 0;
    public final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
        
    
     /**
     * check  installed modules
     * @return 
     */
    public boolean checkInstalledModules() {
        // check install
        ClassLoader loader = IJ.getClassLoader();
        try {
            loader.loadClass("mcib3d.geom.Object3D");
        } catch (ClassNotFoundException e) {
            IJ.log("3D ImageJ Suite not installed, please install from update site");
            return false;
        }
        return true;
    }
    
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
     * Flush and close an image
     */
    public void flush_close(ImagePlus img) {
        img.flush();
        img.close();
    }
    
    
    /**
     * Find images in folder
     */
    public ArrayList findImages(String imagesFolder, String imageExt) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No image found in " + imagesFolder);
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            // Find images with extension
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExt) && !f.startsWith("."))
                images.add(imagesFolder + f);
        }
        Collections.sort(images);
        return(images);
    }
    
     /**
     * Find image type
     */
    public String findImageType(File imagesFolder) {
        String ext = "";
        String[] files = imagesFolder.list();
        for (String name : files) {
            String fileExt = FilenameUtils.getExtension(name);
            switch (fileExt) {
               case "nd" :
                   ext = fileExt;
                   break;
                case "czi" :
                   ext = fileExt;
                   break;
                case "lif"  :
                    ext = fileExt;
                    break;
                case "isc2" :
                    ext = fileExt;
                    break;
                case "tif" :
                    ext = fileExt;
                    break;
            }
        }
        return(ext);
    }
   
   /**
     * Find image calibration
     */
    public Calibration findImageCalib(IMetadata meta) {
        cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
        cal.pixelHeight = cal.pixelWidth;
        if (meta.getPixelsPhysicalSizeZ(0) != null)
            cal.pixelDepth = meta.getPixelsPhysicalSizeZ(0).value().doubleValue();
        else
            cal.pixelDepth = 1;
        cal.setUnit("microns");
        System.out.println("XY calibration = " + cal.pixelWidth + ", Z calibration = " + cal.pixelDepth);
        return(cal);
    }
    
    
    
    /**
     * Find channels name
     * @throws loci.common.services.DependencyException
     * @throws loci.common.services.ServiceException
     * @throws loci.formats.FormatException
     * @throws java.io.IOException
     */
    public String[] findChannels (String imageName, IMetadata meta, ImageProcessorReader reader) throws DependencyException, ServiceException, FormatException, IOException {
        int chs = reader.getSizeC();
        String[] channels = new String[chs];
        String imageExt =  FilenameUtils.getExtension(imageName);
        switch (imageExt) {
            case "nd" :
                for (int n = 0; n < chs; n++) 
                {
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n).toString();
                }
                break;
            case "lif" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null || meta.getChannelName(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n).toString();
                break;
            case "czi" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelFluor(0, n).toString();
                break;
            case "ics" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelExcitationWavelength(0, n).value().toString();
                break;    
            default :
                for (int n = 0; n < chs; n++)
                    channels[n] = Integer.toString(n);
        }
        channelsSize = channels.length;
        return(channels);         
    }
    
    /**
     * Generate dialog box
     */
    public String[] dialog(String[] channels) { 
        
        String[] models = findStardistModels();
        if (!Arrays.asList(models).contains(stardistModelNucleus) || !Arrays.asList(models).contains(stardistModelGenes)) {
            IJ.showMessage("Error", "Missing stardist models");
            return(null);
        }
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets​(0, 80, 0);
        gd.addImage(icon);
        gd.addMessage("Channels", new Font(Font.MONOSPACED , Font.BOLD, 12), Color.blue);
        int index = 0;
        for (int i =0; i < channels.length; i++)
            gd.addChoice(channelsName[i] + " : ", channels, channels[i]);
        // nucleus
        gd.addMessage("Nuclei size filter", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min nucleus volume (µm3) :", minNucVol);
        gd.addNumericField("Max nucleus volume (µm3) :", maxNucVol);
        // genes
        String[] genesDetections = {"Stardist", "DOG"};
        gd.addMessage("Gene segmentation", Font.getFont("Monospace"), Color.blue);
        gd.addChoice("Segmentation method" + " : ", genesDetections, genesDetections[1]);
        gd.addMessage("Gene size filter", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min gene volume (µm3) :", minGeneVol);
        gd.addNumericField("Max gene volume (µm3) :", maxGeneVol); 
        // Background detection
        String[] bgDetections = {"From rois", "Auto", "From calibration"};
        gd.addMessage("Background detection", Font.getFont("Monospace"), Color.blue);
        gd.addChoice("Background method" + " : ", bgDetections, bgDetections[1]);
        gd.addNumericField("background box size :", roiBgSize); 
        gd.addMessage("Background intensity from calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Gene ref intensity : ", calibBgGeneRef); 
        gd.addNumericField("Gene X intensity   : ", calibBgGeneX); 
        if (channels.length > 3)
            gd.addNumericField("Gene Y intensity   : ", calibBgGeneY); 
        gd.addMessage("Single dot intensity from calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Gene ref intensity : ", singleDotIntGeneRef); 
        gd.addNumericField("Gene X intensity   : ", singleDotIntGeneX); 
        if (channels.length > 3)
            gd.addNumericField("Gene Y intensity   : ", singleDotIntGeneY); 
        // Calibration
        gd.addMessage("Image calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("XY calibration (µm):", cal.pixelWidth);
        gd.addNumericField("Z calibration (µm):", cal.pixelDepth);
        gd.showDialog();
        String[] ch = new String[channelsName.length];
        for (int i = 0; i < channelsName.length; i++)
            ch[i] = gd.getNextChoice();
        if(gd.wasCanceled())
            ch = null;
        minNucVol = gd.getNextNumber();
        maxNucVol = gd.getNextNumber();
        genesDetection = gd.getNextChoice();
        minGeneVol = gd.getNextNumber();
        maxGeneVol = gd.getNextNumber();
        bgDetection = gd.getNextChoice();
        roiBgSize = gd.getNextNumber();
        calibBgGeneRef = gd.getNextNumber();
        calibBgGeneX = gd.getNextNumber();
        if (channels.length > 3)
            calibBgGeneY = gd.getNextNumber();
        singleDotIntGeneRef = gd.getNextNumber();
        singleDotIntGeneX = gd.getNextNumber();
        if (channels.length > 3)
            singleDotIntGeneY = gd.getNextNumber();
        cal.pixelWidth = gd.getNextNumber();
        cal.pixelDepth = gd.getNextNumber();
        cal.pixelHeight = cal.pixelWidth;
        pixVol = (float) (cal.pixelWidth*cal.pixelHeight*cal.pixelDepth);
        return(ch);
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
     * Find genes in nucleus
     */
    public int findGenesInNucleus (Objects3DIntPopulation nucPop, Objects3DIntPopulation genesPop) throws IOException {
        MeasurePopulationColocalisation coloc = new MeasurePopulationColocalisation(nucPop, genesPop);
        AtomicInteger ai = new AtomicInteger(0);
        nucPop.getObjects3DInt().forEach(nucObj -> {
            float nucLabel = nucObj.getLabel();
            List<PairObjects3DInt> list = coloc.getPairsObject1(nucLabel, true);
            // no coloc for nucleus nucLabel
            if (list.size() != 0) {
                list.forEach(p -> {
                    Object3DInt geneObj = p.getObject3D2();
                    if (p.getPairValue() > geneObj.size()*0.25) {
                        geneObj.setIdObject(nucLabel);
                        ai.incrementAndGet();
                    }
                });
            }
        });
        return(ai.get());
    } 
    
    /**
     * Find background from rois
     */
    private void findBackgroundRois(ImagePlus imgGene, RoiBg roiBgGene) {
        IJ.showStatus("Finding background from rois ...");
        // find background for geneRef
        imgGene.setRoi(roiBgGene.getRoi());
        ImagePlus imgGeneCrop = imgGene.crop("stack");
        roiBgGene.setBgInt(findBackground(imgGeneCrop, 1,imgGeneCrop.getNSlices()));
        flush_close(imgGeneCrop);
    }
    
    /**
     * Find genes intensity
     */
    private double findGenesIntensity(float nucLabel, Objects3DIntPopulation genePop, ImageHandler imhGene) {
        double geneInt = 0;
        for (Object3DInt obj : genePop.getObjects3DInt()) {
            if (obj.getIdObject() == nucLabel) {
                geneInt += new MeasureIntensity(obj, imhGene).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
            }
        }
        return(geneInt);
    }
    
    /**
     * Find genes volume
     */
    private double findGenesVolume(float nucLabel, Objects3DIntPopulation genePop) {
        double geneVol = 0;
        for (Object3DInt obj : genePop.getObjects3DInt()) {
            if (obj.getIdObject() == nucLabel) {
                geneVol += new MeasureVolume(obj).getValueMeasurement(MeasureVolume.VOLUME_PIX);
            }
        }
        return(geneVol);
    }
    
    /**
     * write results gene spot, Integrated intensity and max spot Integrated intensty ....
     * @param nucleusPop
     * @param geneRefPop
     * @param geneXPop
     * @param geneYPop
     * @param imgGeneRef
     * @param imgGeneX
     * @param imgGeneY
     * @param roiBgGeneRef
     * @param roiBgGeneX
     * @param roiBgGeneY
     * @param imgName
     * @param outPut
     * @throws java.io.IOException
     */
    
    public void writeResults(Objects3DIntPopulation nucleusPop, Objects3DIntPopulation geneRefPop, Objects3DIntPopulation geneXPop,
            Objects3DIntPopulation geneYPop, ImagePlus imgGeneRef, ImagePlus imgGeneX, ImagePlus imgGeneY, 
            RoiBg roiBgGeneRef, RoiBg roiBgGeneX, RoiBg roiBgGeneY, String imgName, BufferedWriter outPut) throws IOException {
        
        ImageHandler imhRef = ImageHandler.wrap(imgGeneRef);
        ImageHandler imhX = ImageHandler.wrap(imgGeneX);
        ImageHandler imhY = (imgGeneY == null) ? null : ImageHandler.wrap(imgGeneY);
        
        // crop image for background in case of manual rois
        if (roiBgGeneRef.getBgInt() == -1) {
            System.out.println("Finding background with manual rois);");
            findBackgroundRois(imgGeneRef, roiBgGeneRef);
            findBackgroundRois(imgGeneX, roiBgGeneX);
            if (imgGeneY == null) 
                findBackgroundRois(imgGeneY, roiBgGeneY);
        }
       
        // For all nuclei compute parameters
        for (Object3DInt nucleusObj : nucleusPop.getObjects3DInt()) {
            float nucLabel = nucleusObj.getLabel();
            
            // calculate nucleus parameters
            double nucleusVol = new MeasureVolume(nucleusObj).getVolumeUnit();
            double nucleusGeneRefInt = new MeasureIntensity(nucleusObj, imhRef).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
            double nucleusGeneXInt = new MeasureIntensity(nucleusObj, imhX).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
            double nucleusGeneYInt = (imgGeneY == null) ? 0 : new MeasureIntensity(nucleusObj, imhY).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
                    
            // find genes Ref in nucleus           
            
            // dots number based on gene channel intensity
            int nbGeneRefDotsNucleusInt = Math.round((float)((nucleusGeneRefInt - roiBgGeneRef.getBgInt() * nucleusObj.size()) / singleDotIntGeneRef));
            int nbGeneXDotsNucleusInt = Math.round((float)((nucleusGeneXInt - roiBgGeneX.getBgInt() * nucleusObj.size()) / singleDotIntGeneX));
            int nbGeneYDotsNucleusInt = (imgGeneY == null) ? 0 : Math.round((float)((nucleusGeneYInt - roiBgGeneY.getBgInt() * nucleusObj.size()) / singleDotIntGeneY));
            
            // dots number based on dots segmented intensity
            double geneRefInt = findGenesIntensity(nucLabel, geneRefPop, imhRef);
            double geneRefVol = findGenesVolume(nucLabel, geneRefPop);
            int nbGeneRefDotsSegInt = Math.round((float)((geneRefInt - roiBgGeneRef.getBgInt() * geneRefVol) / singleDotIntGeneRef));
            double geneXInt = findGenesIntensity(nucLabel, geneXPop, imhX);
            double geneXVol = findGenesVolume(nucLabel, geneXPop);
            int nbGeneXDotsSegInt = Math.round((float)((geneXInt - roiBgGeneX.getBgInt() * geneXVol) / singleDotIntGeneX));
            
            double geneYInt = (imgGeneY == null) ? 0 : findGenesIntensity(nucLabel, geneYPop, imhY);
            double geneYVol = (imgGeneY == null) ? 0 : findGenesVolume(nucLabel, geneYPop);
            int nbGeneYDotsSegInt = (imgGeneY == null) ? 0 : Math.round((float)((geneYInt - roiBgGeneY.getBgInt() * geneYVol) / singleDotIntGeneY));
            
            outPut.write(imgName+"\t"+nucLabel+"\t"+nucleusVol+"\t"+nucleusGeneRefInt+"\t"+roiBgGeneRef.getBgInt()+"\t"+nbGeneRefDotsNucleusInt
                    +"\t"+geneRefVol*pixVol+"\t"+geneRefInt+"\t"+nbGeneRefDotsSegInt+"\t"+nucleusGeneXInt+"\t"+roiBgGeneX.getBgInt()+"\t"+nbGeneXDotsNucleusInt
                    +"\t"+geneXVol*pixVol+"\t"+geneXInt+"\t"+nbGeneXDotsSegInt+"\t"+nucleusGeneYInt+"\t"+roiBgGeneY.getBgInt()+"\t"+nbGeneYDotsNucleusInt
                    +"\t"+geneYVol*pixVol+"\t"+geneYInt+"\t"+nbGeneYDotsSegInt+"\t"+"\n");
            outPut.flush();
        }
    }
    
    /**
     * Remove object with only one plan
     * @param pop
     */
    public void popFilterOneZ(Objects3DIntPopulation pop) {
        pop.getObjects3DInt().removeIf(p -> (p.getObject3DPlanes().size() == 1));
        pop.resetLabels();
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
     * Remove object with size < min and size > max
     * @param pop
     * @param min
     * @param max
     */
    public void popFilterSize(Objects3DIntPopulation pop, double min, double max) {
        pop.getObjects3DInt().removeIf(p -> (new MeasureVolume(p).getVolumeUnit() < min) || (new MeasureVolume(p).getVolumeUnit() > max));
        pop.resetLabels();
    }
    
     /**
     * Threshold 
     * USING CLIJ2
     * @param imgCL
     * @param thMed
     * @param fill 
     */
    private ClearCLBuffer threshold(ClearCLBuffer imgCL, String thMed) {
        ClearCLBuffer imgCLBin = clij2.create(imgCL);
        clij2.automaticThreshold(imgCL, imgCLBin, thMed);
        return(imgCLBin);
    }
    
    /**
     * return objects population in an binary image
     * Using CLIJ2
     * @param imgCL
     * @return pop
     */

    private Objects3DIntPopulation getPopFromClearBuffer(ClearCLBuffer imgCL) {
        ClearCLBuffer output = clij2.create(imgCL);
        clij2.connectedComponentsLabelingBox(imgCL, output);
        ImagePlus imgLab  = clij2.pull(output);
        clij2.release(output);
        Objects3DIntPopulation pop = new Objects3DIntPopulation(ImageHandler.wrap(imgLab));
        return pop;
    }  
    
        
    /**
     * Find gene population
     * @param imgGene
     * @return genePop
     */
    public Objects3DIntPopulation findGenePop(ImagePlus imgGene) {
        ClearCLBuffer imgCL = clij2.push(imgGene);
        ClearCLBuffer imgCLMed = medianFilter(imgCL, 1, 1, 1);
        clij2.release(imgCL);
        ClearCLBuffer imgCLDOG = DOG(imgCLMed, DOGMin, DOGMin, DOGMin, DOGMax, DOGMax, DOGMax);
        clij2.release(imgCLMed);
        ClearCLBuffer imgCLBin = threshold(imgCLDOG, "Triangle"); 
        clij2.release(imgCLDOG);
        Objects3DIntPopulation genePop = getPopFromClearBuffer(imgCLBin);
        popFilterSize(genePop, minGeneVol/pixVol, maxGeneVol/pixVol);
        genePop.setVoxelSizeXY(cal.pixelWidth);
        genePop.setVoxelSizeZ(cal.pixelDepth); 
        clij2.release(imgCLBin);       
        return(genePop);
    }
    
        
    /**
     * Find gene population with Stardist
     */
    public Objects3DIntPopulation stardistGenePop(ImagePlus imgGene) throws IOException{

        cal = imgGene.getCalibration();
        ClearCLBuffer imgCL = clij2.push(imgGene);
        ClearCLBuffer imgCLMed = medianFilter(imgCL, 1, 1, 1);
        clij2.release(imgCL);
        ImagePlus imgGeneMed = clij2.pull(imgCLMed);
        clij2.release(imgCLMed);
        imgGeneMed.show();
        // Go StarDist
        File starDistModelFile = new File(modelsPath+File.separator+stardistModelGenes);
        StarDist2D star = new StarDist2D(syncObject, starDistModelFile);
        star.loadInput(imgGeneMed);
        star.setParams(stardistPercentileBottom, stardistPercentileTop, stardistProbThreshDots, stardistOverlayThreshDots, stardistOutput);
        star.run();
        // label in 3D
        ImagePlus imgGeneLab = star.associateLabels();
        imgGeneLab.setCalibration(cal);
        Objects3DIntPopulation genePop = new Objects3DIntPopulation(ImageInt.wrap(imgGeneLab));
        popFilterSize(genePop, minGeneVol, maxGeneVol);
        return(genePop);
}
    
     
        /** Look for all nuclei
         Do z slice by slice stardist 
         * return nuclei population
         */
        public Objects3DIntPopulation stardistNucleiPop(ImagePlus imgNuc) throws IOException{
            cal = imgNuc.getCalibration();
            // resize to be in a stardist-friendly scale
            int width = imgNuc.getWidth();
            int height = imgNuc.getHeight();
            float factor = 0.25f;
            ImagePlus img = imgNuc.resize((int)(width*factor), (int)(height*factor), 1, "none");
            IJ.run(img, "Remove Outliers", "block_radius_x=10 block_radius_y=10 standard_deviations=1 stack");
            // Go StarDist
            File starDistModelFile = new File(modelsPath+File.separator+stardistModelNucleus);
            StarDist2D star = new StarDist2D(syncObject, starDistModelFile);
            star.loadInput(img);
            star.setParams(stardistPercentileBottom, stardistPercentileTop, stardistProbThreshNuc, stardistOverlayThreshNuc, stardistOutput);
            star.run();
            flush_close(img);
            // label in 3D
            ImagePlus nuclei = star.associateLabels().resize(width, height, 1, "none");
            ImageInt label3D = ImageInt.wrap(nuclei);
            label3D.setCalibration(cal);        
            Objects3DIntPopulation nucPop = new Objects3DIntPopulation(label3D);
            System.out.println(nucPop.getNbObjects() + " nucleus detections");
            popFilterOneZ(nucPop);
            popFilterSize(nucPop, minNucVol, maxNucVol);
            flush_close(nuclei);
            return(nucPop);
        }
    
    
    /**
     * Find min background roi
     * @param img
     * @param size
     * @return 
     */
    public RoiBg findRoiBackgroundAuto(ImagePlus img) {
        // scroll gene image and measure bg intensity in roi 
        // take roi lower intensity
        
        ArrayList<RoiBg> intBgFound = new ArrayList<>();
        for (int x = 0; x < img.getWidth() - roiBgSize; x += roiBgSize) {
            for (int y = 0; y < img.getHeight() - roiBgSize; y += roiBgSize) {
                Roi roi = new Roi(x, y, roiBgSize, roiBgSize);
                img.setRoi(roi);
                ImagePlus imgCrop = img.crop("stack");
                double bg = findBackground(imgCrop,1,img.getNSlices());
                intBgFound.add(new RoiBg(roi, bg));
                flush_close(imgCrop);
            }
        }
        img.deleteRoi();
        // sort RoiBg on bg value
        intBgFound.sort(Comparator.comparing(RoiBg::getBgInt));
        // Find lower value
        RoiBg roiBg = intBgFound.get(0);
        
        int roiCenterX = (int)(roiBg.getRoi().getBounds().x+(roiBgSize/2));
        int roiCenterY = (int)(roiBg.getRoi().getBounds().y+(roiBgSize/2));
        System.out.println("Roi auto background found = "+roiBg.getBgInt()+" center x = "+roiCenterX+", y = "+roiCenterY);
        return(roiBg);
    }
    
    /**
     * Do Z projection
     * @param img
     * @param projection parameter
     */
    public ImagePlus doZProjection(ImagePlus img, int zMin, int zMax, int param) {
        ZProjector zproject = new ZProjector();
        zproject.setMethod(param);
        zproject.setStartSlice(zMin);
        zproject.setStopSlice(zMax);
        zproject.setImage(img);
        zproject.doProjection();
       return(zproject.getProjection());
    }
    
    /**
     * Find background image intensity:
     * Z projection over min intensity + read mean intensity
     * @param img
     */
    public double findBackground(ImagePlus img, int zMin, int zMax) {
      ImagePlus imgProj = doZProjection(img, zMin, zMax, ZProjector.MIN_METHOD);
      ImageProcessor imp = imgProj.getProcessor();
      double bg = imp.getStatistics().median;
      //System.out.println("Background = " + bg);
      flush_close(imgProj);
      return(bg);
    }
    
    
    /**
     * 
     * @param xmlFile
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public ArrayList<Point3DInt> readXML(String xmlFile) throws ParserConfigurationException, SAXException, IOException {
        ArrayList<Point3DInt> ptList = new ArrayList<>();
        int x = 0, y = 0 ,z = 0;
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
                x = (int)Double.parseDouble(eElement.getElementsByTagName("MarkerX").item(0).getTextContent());
                y = (int)Double.parseDouble(eElement.getElementsByTagName("MarkerY").item(0).getTextContent());
                z = (int)Double.parseDouble(eElement.getElementsByTagName("MarkerZ").item(0).getTextContent());
            }
            Point3DInt pt = new Point3DInt(x, y, z);
            ptList.add(pt);
        }
        return(ptList);
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
    public void saveDotsImage (ImagePlus img, Objects3DIntPopulation nucPop, Objects3DIntPopulation geneRefPop, Objects3DIntPopulation geneXPop,
            Objects3DIntPopulation geneYPop, String outDirResults, String rootName) {
        // red dots geneRef , dots green geneX, blue nucDilpop
        ImageHandler imgNucleus = ImageHandler.wrap(img).createSameDimensions();
        ImageHandler imgDotsGeneRef = ImageHandler.wrap(img).createSameDimensions();
        ImageHandler imgDotsGeneX = ImageHandler.wrap(img).createSameDimensions();
        ImageHandler imgDotsGeneY = (channelsSize > 3) ? ImageHandler.wrap(img).createSameDimensions() : null;
        // draw nucleus dots population
        nucPop.drawInImage(imgNucleus);
        for (Object3DInt obj : geneRefPop.getObjects3DInt()) {
            if (obj.getIdObject() != 0)
                obj.drawObject(imgDotsGeneRef, 255);
        }
        for (Object3DInt obj : geneXPop.getObjects3DInt()) {
            if (obj.getIdObject() != 0)
                obj.drawObject(imgDotsGeneX, 255);
        }
        if (geneYPop.getNbObjects() == 0) {
            for (Object3DInt obj : geneYPop.getObjects3DInt()) {
                if (obj.getIdObject() != 0)
                    obj.drawObject(imgDotsGeneY, 255);
            }
        }
        ImagePlus[] imgColors = {imgDotsGeneRef.getImagePlus(), imgDotsGeneX.getImagePlus(), imgNucleus.getImagePlus(), null,imgDotsGeneY.getImagePlus()};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgObjects.setCalibration(cal);
        IJ.run(imgObjects, "Enhance Contrast", "saturated=0.35");

        // Save images
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(outDirResults + rootName + "_DotsObjects.tif");
        imgNucleus.closeImagePlus();
        imgDotsGeneRef.closeImagePlus();
        imgDotsGeneX.closeImagePlus();
    }
}
