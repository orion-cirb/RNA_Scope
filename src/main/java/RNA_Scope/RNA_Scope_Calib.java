/*
 * Calibration steps for RNAScope
 * Analyze background on rois and dots volume, intensity using xml file 
 * in images containing single dots
 */
package RNA_Scope;


import RNA_Scope_Utils.Dot;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom.Point3D;
import mcib3d.geom.Point3DInt;
import mcib3d.geom2.BoundingBox;
import mcib3d.geom2.Object3DComputation;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.image3d.ImageHandler;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;


/**
 *
 * @author phm
 */
public class RNA_Scope_Calib implements PlugIn {
    
private String imageDir = "";
private String outDirResults = "";
private final Calibration cal = new Calibration();   
private BufferedWriter output_dotCalib;
private RNA_Scope_Utils.RNA_Scope_Tools tools = new RNA_Scope_Utils.RNA_Scope_Tools();

    /**
     * Find pointed single dots in dotsPop population
     * @param arg 
     */
    private ArrayList<Dot> findSingleDots(ArrayList<Point3DInt> pts, Objects3DIntPopulation dotsPop, Objects3DIntPopulation pointedDotsPop, ImagePlus img) {
        ImageHandler imh = ImageHandler.wrap(img);
        ArrayList<Dot> dots = new ArrayList();
        int index = 0;
        for (Object3DInt dotObj : dotsPop.getObjects3DInt()) {
            BoundingBox bbox = dotObj.getBoundingBox();
            for (Point3DInt pt : pts) {
                if(bbox.contains(pt)) {
                    double dotVol = new MeasureVolume(dotObj).getVolumePix();
                    double dotInt = new MeasureIntensity(dotObj, imh).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
                    int zMin = bbox.zmin + 1;
                    int zMax = bbox.zmax + 1;
                    double center = bbox.zmax - bbox.zmin;
                    Dot dot = new Dot(index, dotVol, dotInt, zMin, zMax, center);
                    dots.add(dot);
                    pointedDotsPop.addObject(dotObj);
                }
                
            }
            index++;
        }
        return(dots);
    }
    
    
    /**
     * Label object
     * @param popObj
     * @param img 
     */
    private void labelsObject (Objects3DIntPopulation popObj, ImagePlus img, int fontSize) {
        Font tagFont = new Font("SansSerif", Font.PLAIN, fontSize);
        for (Object3DInt obj : popObj.getObjects3DInt()) {
            BoundingBox box = obj.getBoundingBox();
            int z = box.zmax - box.zmin;
            int x = box.xmin - 2;
            int y = box.ymin - 2;
            img.setSlice(z+1);
            ImageProcessor ip = img.getProcessor();
            ip.setFont(tagFont);
            ip.setColor(255);
            ip.drawString(String.valueOf(obj.getLabel()), x, y);
            img.updateAndDraw();
        }
    }
    
     /**
     * save images objects population
     * @param imgNuc
     * @param dotsPop
     * @param outDirResults
     * @param rootName
     */
    public void saveDotsImage (ImagePlus img, Objects3DIntPopulation dotsAllPop, Objects3DIntPopulation dotsPop, String outDirResults, String rootName) {
        // red dots geneRef , dots green geneX, blue nucDilpop
        ImageHandler imgDots = ImageHandler.wrap(img).createSameDimensions();
        ImageHandler imgAllDots = imgDots.createSameDimensions();
        // draw dots population
        dotsPop.drawInImage(imgDots);
        dotsAllPop.drawInImage(imgAllDots);
        ImagePlus[] imgColors = {imgAllDots.getImagePlus(), imgDots.getImagePlus(), null, img.duplicate()};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        IJ.run(imgObjects, "Enhance Contrast", "saturated=0.35");

        // Save images
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(outDirResults + rootName + "_DotsObjects.tif");
        imgDots.closeImagePlus();
    }
    
    
    
    
  @Override
    public void run(String arg) {
        try {
            imageDir = IJ.getDirectory("Choose directory containing tif, roi and xml files...");
            if (imageDir == null) {
                return;
            }
            File inDir = new File(imageDir);
            String[] imageFile = inDir.list();
            if (imageFile == null) {
                System.out.println("No Image found in "+imageDir);
                return;
            }
            
            Arrays.sort(imageFile);
            int imageNum = 0; 
            String rootName = "";
            ArrayList<String> ch = new ArrayList<>();
            for (String f : imageFile) {
                // Find tif files no dconv TIF deconv
                String fileExt = FilenameUtils.getExtension(f);
                 if (fileExt.equals("tif")) {
                    rootName = FilenameUtils.getBaseName(f);
                    String imageName = inDir+ File.separator+f;
                    imageNum++;
                    
                    // Check calibration
                    if (imageNum == 1) {
                        // create output folder
                        outDirResults = inDir + File.separator+ "Results"+ File.separator;
                        File outDir = new File(outDirResults);
                        if (!Files.exists(Paths.get(outDirResults))) {
                            outDir.mkdir();
                        } 
                        // write result file headers
                        FileWriter  fwAnalyze_detail = new FileWriter(outDirResults + "dotsCalibration_results.xls",false);
                        output_dotCalib = new BufferedWriter(fwAnalyze_detail);
                        // write results headers
                        output_dotCalib.write("Image Name\t#Dot\tDot Vol (pixel3)\tDot Integrated Intensity\tMedian Dot Background intensity\t"
                                + "Corrected Dots Integrated Intensity\tDot Z center\tDot Z range\tMean intensity per single dot\n");
                        output_dotCalib.flush();
                    }
                    
                    // Find roi file name
                    String roiFile = inDir+ File.separator + rootName + ".zip";
                    if (!new File(roiFile).exists()) {
                        IJ.showStatus("No roi file found !") ;
                        return;
                    }
                    // Find roi file name
                    String xmlFile = inDir+ File.separator + rootName + ".xml";
                    if (!new File(xmlFile).exists()) {
                        IJ.showStatus("No xml file found !") ;
                        return;
                    }
                    else {                        
                        // Open Gene reference channel
                        System.out.println("Opening gene channel ...");
                        ImagePlus img = IJ.openImage(imageName);
                        RoiManager rm = new RoiManager(false);
                        rm.runCommand("Open", roiFile);
                        
                        // Read dots coordinates in xml file
                        ArrayList<Point3DInt> dotsCenter = tools.readXML(xmlFile);
                        System.out.println("Pointed dots found = "+dotsCenter.size());
                        
                        // 3D dots segmentation
                        Objects3DIntPopulation dotsPop = tools.stardistGenePop(img, null);
                        System.out.println("Total dots found = "+dotsPop.getNbObjects());
                        
                        // find pointed dots in dotsPop
                        Objects3DIntPopulation pointedDotsPop = new Objects3DIntPopulation();

                        ArrayList<Dot> dots = findSingleDots(dotsCenter, dotsPop, pointedDotsPop, img);
                        System.out.println("Associated dots = "+dots.size());
                        
                        // Save dots
                        saveDotsImage (img, dotsPop, pointedDotsPop, outDirResults, rootName);
                        
                        // for all rois
                        // find background associated to dot
                        double sumCorIntDots = 0;
                        for (Dot dot : dots) {
                            Roi roi = rm.getRoi(dot.getIndex());
                            img.setRoi(roi);
                            ImagePlus imgCrop = img.crop("stack");
                            double bgDotInt = tools.findBackground(imgCrop, dot.getZmin(), dot.getZmax());
                            double corIntDot = dot.getIntDot() - (bgDotInt * dot.getVolDot());
                            sumCorIntDots += corIntDot;
                            // write results
                            output_dotCalib.write(rootName+"\t"+dot.getIndex()+"\t"+dot.getVolDot()+"\t"+dot.getIntDot()+"\t"+bgDotInt+"\t"+corIntDot+
                                    "\t"+dot.getZCenter()+"\t"+(dot.getZmax()-dot.getZmin())+"\n");
                            output_dotCalib.flush();
                            tools.flush_close(imgCrop);
                        }
                        double MeanIntDot = sumCorIntDots / rm.getCount();
                        output_dotCalib.write("\t\t\t\t\t\t\t\t"+MeanIntDot+"\n");
                    }
                }
            }
            if(new File(outDirResults + "dotsCalibration_results.xls").exists())
                output_dotCalib.close();
            IJ.showStatus("Calibration done");
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(RNA_Scope_Calib.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

                    
                              
}
