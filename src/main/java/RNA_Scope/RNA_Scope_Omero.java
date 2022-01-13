/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RNA_Scope;


import static RNA_Scope.RNA_Scope_JDialog.imageData;
import static RNA_Scope.RNA_Scope_JDialog.selectedDataset;
import static RNA_Scope.RNA_Scope_JDialog.selectedProject;
import RNA_Scope_Utils.Cell;
import static RNA_Scope_Utils.OmeroConnect.addImageToDataset;
import static RNA_Scope_Utils.OmeroConnect.getFileAnnotations;
import static RNA_Scope_Utils.OmeroConnect.addFileAnnotation;
import static RNA_Scope_Utils.OmeroConnect.gateway;
import static RNA_Scope_Utils.OmeroConnect.getImageZ;
import static RNA_Scope_Utils.OmeroConnect.securityContext;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import mcib3d.geom.Objects3DPopulation;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.ChannelData;
import omero.gateway.model.FileAnnotationData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import org.apache.commons.lang.ArrayUtils;
import org.xml.sax.SAXException;

/**
 *
 * @author phm
 */

 // Images on OMERO server

public class RNA_Scope_Omero implements PlugIn {
    

    private String tempDir = System.getProperty("java.io.tmpdir");
    private String outDirResults = tempDir+File.separator+"resulst.xls";
    private String imageExtension = ".nd";
    private RNA_Scope.RNA_Scope_Main main = new RNA_Scope.RNA_Scope_Main();
    private RNA_Scope_Utils.RNA_Scope_Processing process =  new  RNA_Scope_Utils.RNA_Scope_Processing();
    
    
    
    @Override
    public void run(String arg) {
        try {
            ArrayList<String> ch = new ArrayList();
            // initialize results files
            process.InitResults(outDirResults);
            
            for (ImageData image : imageData) {
                if (image.getName().endsWith(".nd")) {
                    main.rootName = image.getName().replace(".nd", "");
                    PixelsData pixels = image.getDefaultPixels();
                    int sizeZ = pixels.getSizeZ();
                    int sizeC = pixels.getSizeC();
                    MetadataFacility mf = gateway.getFacility(MetadataFacility.class);
                    String[] channels = new String[sizeC];
                    for(ChannelData chs : mf.getChannelData(securityContext, image.getId())) {
                        channels[chs.getIndex()] = chs.getChannelLabeling();
                    }

                    try {                        
                        int zStart = main.removeSlice;
                        int zStop = (sizeZ - 2 * main.removeSlice) <= 0 ? sizeZ : sizeZ - main.removeSlice;
                        
                        /*
                        * Open Channel 1 (gene reference)
                        */
                        int channelIndex = ArrayUtils.indexOf(channels, ch.get(1));
                        System.out.println("-- Opening gene reference channel : "+ ch.get(1));
                        ImagePlus imgGeneRef = getImageZ(image, 1, channelIndex + 1, zStart, zStop).getImagePlus();
                        
                        // Find gene reference dots
                        Objects3DPopulation geneRefDots = new Objects3DPopulation();
                        if (main.geneSegMethod.equals("StarDist"))
                            geneRefDots = process.stardistGenePop(imgGeneRef, null);
                        else
                            geneRefDots = process.findGenePop(imgGeneRef, null);
                        System.out.println("Finding gene "+geneRefDots.getNbObjects()+" reference dots");

                        //Find gene X dots                     
                        
                        /*
                        * Open Channel 3 (gene X)
                        */
                        channelIndex = ArrayUtils.indexOf(channels, ch.get(2));
                        System.out.println("-- Opening gene X channel : " + ch.get(2));
                        ImagePlus imgGeneX = getImageZ(image, 1, channelIndex + 1, zStart, zStop).getImagePlus();

                        // Find gene X dots
                        Objects3DPopulation geneXDots = new Objects3DPopulation();
                        if (main.geneSegMethod.equals("StarDist"))
                            geneXDots = process.stardistGenePop(imgGeneX, null);
                        else
                            geneXDots = process.findGenePop(imgGeneX, null);
                        System.out.println("Finding gene "+geneXDots.getNbObjects()+" X dots");
                        
                        // find background from roi
                        Roi roiGeneRef = null, roiGeneX = null;
                        
                        // Background detection methods
                        
                        switch (main.autoBackground) {
                            // from rois
                            case "From roi" :
                                if (image.getAnnotations().isEmpty()) {
                                    IJ.showStatus("No roi file found !");
                                    return;
                                }
                                List<FileAnnotationData> fileAnnotations = getFileAnnotations(image, null);
                                // If exists roi in image
                                String roiFile = main.rootName + ".zip";
                                // Find roi for gene ref and gene X
                                RoiManager rm = new RoiManager(false);
                                rm.runCommand("Open", roiFile);

                                for (int r = 0; r < rm.getCount(); r++) {
                                    Roi roi = rm.getRoi(r);
                                    if (roi.getName().equals("generef"))
                                        roiGeneRef = roi;
                                    else
                                        roiGeneX = roi;
                                }
                                break;
                            // automatic search roi from calibration values     
                            case "Auto" :
                                roiGeneRef = process.findRoiBackgroundAuto(imgGeneRef, main.calibBgGeneRef);
                                roiGeneX = process.findRoiBackgroundAuto(imgGeneX, main.calibBgGeneX);
                                break;
                            case "From calibration" :
                                roiGeneRef = null;
                                roiGeneX = null;
                                break;
                        }

                        /*
                        * Open DAPI channel
                        */
                        channelIndex = ArrayUtils.indexOf(channels, ch.get(0));
                        System.out.println("-- Opening Nucleus channel : "+ ch.get(0));
                        ImagePlus imgNuc = getImageZ(image, 1, channelIndex + 1, zStart, zStop).getImagePlus();


                        Objects3DPopulation cellsPop = new Objects3DPopulation();
                        if (main.nucSegMethod.equals("StarDist"))
                            cellsPop = process.stardistNucleiPop(imgNuc);
                        else
                            cellsPop = process.findNucleus(imgNuc);

                        // Find cells parameters in geneRef and geneX images
                        ArrayList<Cell> listCells = process.tagsCells(cellsPop, geneRefDots, geneXDots, imgGeneRef, imgGeneX, roiGeneRef, roiGeneX);


                        // write results for each cell population
                        for (int n = 0; n < listCells.size(); n++) {
                            main.output_detail_Analyze.write(main.rootName+"\t"+listCells.get(n).getIndex()+"\t"+listCells.get(n).getCellVol()+"\t"+listCells.get(n).getzCell()+"\t"+listCells.get(n).getCellGeneRefInt()
                                    +"\t"+listCells.get(n).getCellGeneRefBgInt()+"\t"+listCells.get(n).getnbGeneRefDotsCellInt()+"\t"+listCells.get(n).getGeneRefDotsVol()+"\t"+listCells.get(n).getGeneRefDotsInt()
                                    +"\t"+listCells.get(n).getnbGeneRefDotsSegInt()+"\t"+listCells.get(n).getCellGeneXInt()+"\t"+listCells.get(n).getCellGeneXBgInt()+"\t"+listCells.get(n).getnbGeneXDotsCellInt()
                                    +"\t"+listCells.get(n).getGeneXDotsVol()+"\t"+listCells.get(n).getGeneXDotsInt()+"\t"+listCells.get(n).getnbGeneXDotsSegInt()+"\n");
                            main.output_detail_Analyze.flush();                       

                        }

                        // import  to Omero server
                        addImageToDataset(selectedProject, selectedDataset, outDirResults,  main.rootName + "_Objects.tif", true);
                        new File(outDirResults +  main.rootName + "_Objects.tif").delete();

                        // save random color nucleus popualation
                        process.saveCells(imgNuc, cellsPop, outDirResults,  main.rootName);

                        // import to Omero server
                        addImageToDataset(selectedProject, selectedDataset, outDirResults,  main.rootName + "_Nucleus-ColorObjects.tif", true);
                        new File(outDirResults +  main.rootName + "_Nucleus-ColorObjects.tif").delete();
                        
                        // save dots segmentations
                        process.saveDotsImage (imgNuc, cellsPop, geneRefDots, geneXDots, outDirResults,  main.rootName);
                        
                        // import to Omero server
                        addImageToDataset(selectedProject, selectedDataset, outDirResults,  main.rootName + "_DotsObjects.tif", true);
                        new File(outDirResults +  main.rootName + "_DotsObjects.tif").delete();

                        process.closeImages(imgNuc);
                        process.closeImages(imgGeneRef);
                        process.closeImages(imgGeneX);
                        

                    } catch (DSOutOfServiceException | ExecutionException | DSAccessException | ParserConfigurationException | SAXException | IOException ex) {
                        Logger.getLogger(RNA_Scope_Omero.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(RNA_Scope_Omero.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if (new File(outDirResults + "detailed_results.xls").exists())
                main.output_detail_Analyze.close();
            
            // Attach results file to image
            File fileResults = new File(outDirResults);
            addFileAnnotation(imageData.get(0), fileResults, "text/csv", "Results");
            fileResults.delete();
        } catch (ExecutionException | DSAccessException | DSOutOfServiceException | IOException ex) {
            Logger.getLogger(RNA_Scope_Omero.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
}
