package RNA_Scope;


import static RNA_Scope.RNA_Scope_Main.autoBackground;
import static RNA_Scope.RNA_Scope_Main.cal;
import static RNA_Scope.RNA_Scope_Main.calibBgGeneRef;
import static RNA_Scope.RNA_Scope_Main.calibBgGeneX;
import static RNA_Scope.RNA_Scope_Main.channels;
import static RNA_Scope.RNA_Scope_Main.imagesFiles;
import static RNA_Scope.RNA_Scope_Main.imagesFolder;
import static RNA_Scope.RNA_Scope_Main.output_detail_Analyze;
import static RNA_Scope.RNA_Scope_Main.removeSlice;
import RNA_Scope_Utils.Cell;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.util.ImageProcessorReader;
import loci.plugins.BF;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import java.util.ArrayList;
import loci.plugins.in.ImporterOptions;
import mcib3d.geom.Objects3DPopulation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import static RNA_Scope_Utils.RNA_Scope_Processing.InitResults;
import static RNA_Scope_Utils.RNA_Scope_Processing.closeImages;
import static RNA_Scope_Utils.RNA_Scope_Processing.findGenePop;
import static RNA_Scope_Utils.RNA_Scope_Processing.findNucleus;
import static RNA_Scope_Utils.RNA_Scope_Processing.findRoiBackgroundAuto;
import static RNA_Scope_Utils.RNA_Scope_Processing.saveCells;
import static RNA_Scope_Utils.RNA_Scope_Processing.saveCellsLabelledImage;
import static RNA_Scope_Utils.RNA_Scope_Processing.saveDotsImage;
import static RNA_Scope_Utils.RNA_Scope_Processing.tagsCells;


/**
 *
 * @author phm
 */
public class RNA_Scope_Local implements PlugIn {
    
    
    @Override
    public void run(String arg) {
            try {
                // create output folder
                String outDirResults = imagesFolder + File.separator+ "Results"+ File.separator;
                File outDir = new File(outDirResults);
                if (!Files.exists(Paths.get(outDirResults))) {
                    outDir.mkdir();
                }
                // initialize results files
                InitResults(outDirResults);
                String rootName = "";
                
                // create OME-XML metadata store of the latest schema version
                ServiceFactory factory;
                factory = new ServiceFactory();
                OMEXMLService service = factory.getInstance(OMEXMLService.class);
                IMetadata meta = service.createOMEXMLMetadata();
                ImageProcessorReader reader = new ImageProcessorReader();
                reader.setMetadataStore(meta);
                for (String f : imagesFiles) {
                    rootName = FilenameUtils.getBaseName(f);
                    reader.setId(f);
                    reader.setSeries(0);
                    int sizeC = reader.getSizeC();
                    int sizeZ = reader.getSizeZ();
                    cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
                    cal.pixelHeight = cal.pixelWidth;
                    String channelsID = meta.getImageName(0);
                    String[] chs = channelsID.replace("_", "-").split("/");
                    ImporterOptions options = new ImporterOptions();
                    options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                    options.setId(f);
                    options.setSplitChannels(true);
                    options.setZBegin(0, removeSlice);
                    if (2 * removeSlice < sizeZ)
                        options.setZEnd(0, sizeZ-1  - removeSlice);
                    options.setZStep(0, 1);
                    options.setQuiet(true);

                    /*
                    * Open Channel 1 (gene reference)
                    */
                    int channelIndex = ArrayUtils.indexOf(chs, channels.get(1)) + 1;
                    String imgChName = imagesFolder + File.separatorChar+rootName + "_w" + channelIndex + channels.get(1)+ ".TIF";
                    
                    System.out.println("-- Opening gene reference channel : "+ channels.get(1));
                    ImagePlus imgGeneRef = IJ.openImage(imgChName);
                    imgGeneRef.setCalibration(cal);
                    
                    /*
                    * Open Channel 3 (gene X)
                    */
                    channelIndex = ArrayUtils.indexOf(chs, channels.get(2)) + 1;
                    imgChName = imagesFolder + File.separatorChar+rootName + "_w" + channelIndex + channels.get(2)+ ".TIF";
                    System.out.println("-- Opening gene X channel : " + channels.get(2));
                    ImagePlus imgGeneX = IJ.openImage(imgChName);
                    imgGeneX.setCalibration(cal);

                    Roi roiGeneRef = null, roiGeneX = null;

                    // Background detection methods

                    switch (autoBackground) {
                        // from rois
                        case "From rois" :
                            String roiFile = imagesFolder+ File.separator + rootName + ".zip";
                            if (!new File(roiFile).exists()) {
                                IJ.showStatus("No roi file found !");
                                return;
                            }
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
                            roiGeneRef = findRoiBackgroundAuto(imgGeneRef, calibBgGeneRef);
                            roiGeneX = findRoiBackgroundAuto(imgGeneX, calibBgGeneX);
                            break;
                        case "From calibration" :
                            roiGeneRef = null;
                            roiGeneX = null;
                            break;
                    }

                    // Find gene reference dots
                    Objects3DPopulation geneRefDots = findGenePop(imgGeneRef, null, "Isodata");
                    System.out.println("Finding gene "+geneRefDots.getNbObjects()+" reference dots");

                    //Find gene X dots
                    Objects3DPopulation geneXDots = findGenePop(imgGeneX, null, "Isodata");
                    System.out.println("Finding gene "+geneXDots.getNbObjects()+" X dots");

                    /*
                    * Open DAPI channel
                    */
                    channelIndex = ArrayUtils.indexOf(chs, channels.get(0)) + 1;
                    imgChName = imagesFolder + File.separator + rootName + "_w" + channelIndex+ channels.get(0)+ ".TIF";
                    System.out.println("-- Opening Nucleus channel : "+ channels.get(0));
                    ImagePlus imgNuc = IJ.openImage(imgChName);
                    imgNuc.setCalibration(cal);
                    
                    // if no dilatation find cells with cellOutliner on gene reference image
                    // else dilate nucleus

                    Objects3DPopulation cellsPop = findNucleus(imgNuc);

                    // Find cells parameters in geneRef and geneX images
                    ArrayList<Cell> listCells = tagsCells(cellsPop, geneRefDots, geneXDots, imgGeneRef, imgGeneX, roiGeneRef, roiGeneX);

                    // write results for each cell population
                    for (int n = 0; n < listCells.size(); n++) {
                        output_detail_Analyze.write(rootName+"\t"+listCells.get(n).getIndex()+"\t"+listCells.get(n).getCellVol()+"\t"+listCells.get(n).getzCell()+"\t"+listCells.get(n).getCellGeneRefInt()
                                +"\t"+listCells.get(n).getCellGeneRefBgInt()+"\t"+listCells.get(n).getnbGeneRefDotsCellInt()+"\t"+listCells.get(n).getGeneRefDotsVol()+"\t"+listCells.get(n).getGeneRefDotsInt()
                                +"\t"+listCells.get(n).getnbGeneRefDotsSegInt()+"\t"+listCells.get(n).getCellGeneXInt()+"\t"+listCells.get(n).getCellGeneXBgInt()+"\t"+listCells.get(n).getnbGeneXDotsCellInt()
                                +"\t"+listCells.get(n).getGeneXDotsVol()+"\t"+listCells.get(n).getGeneXDotsInt()+"\t"+listCells.get(n).getnbGeneXDotsSegInt()+"\n");
                        output_detail_Analyze.flush();                       

                    }
                    
                    // Save labelled nucleus
                    saveCellsLabelledImage(imgNuc, cellsPop, imgGeneRef, imgGeneX, outDirResults, rootName);

                    // save random color nucleus popualation
                    saveCells(imgNuc, cellsPop, outDirResults, rootName);

                    // save dots segmented objects
                    saveDotsImage (imgNuc, cellsPop, geneRefDots, geneXDots, outDirResults, rootName);

                    closeImages(imgNuc);
                    closeImages(imgGeneRef);
                    closeImages(imgGeneX);
                }
                if (new File(outDirResults + "detailed_results.xls").exists())
                    output_detail_Analyze.close();
                } catch (IOException | DependencyException | ServiceException | FormatException ex) {
                    Logger.getLogger(RNA_Scope_Local.class.getName()).log(Level.SEVERE, null, ex);
                }

            IJ.showStatus("Process done ...");
        }
    
    
}
