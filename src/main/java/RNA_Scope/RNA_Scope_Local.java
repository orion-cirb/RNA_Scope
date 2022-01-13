package RNA_Scope;



import RNA_Scope_Utils.Cell;
import RNA_Scope_Utils.Image_Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
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
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import java.util.ArrayList;
import loci.plugins.in.ImporterOptions;
import mcib3d.geom.Objects3DPopulation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;



/**
 *
 * @author phm
 */
public class RNA_Scope_Local implements PlugIn {

private RNA_Scope.RNA_Scope_Main main = new RNA_Scope.RNA_Scope_Main();
private RNA_Scope_Utils.RNA_Scope_Processing process =  new  RNA_Scope_Utils.RNA_Scope_Processing();
private Calibration cal;

    @Override
    public void run(String arg) {
            try {
                // create output folder
                String outDirResults = main.imagesFolder + File.separator+ "Results"+ File.separator;
                File outDir = new File(outDirResults);
                if (!Files.exists(Paths.get(outDirResults))) {
                    outDir.mkdir();
                }
                // initialize results files
                process.InitResults(outDirResults);
                String rootName = "";
                
                // create OME-XML metadata store of the latest schema version
                ServiceFactory factory;
                factory = new ServiceFactory();
                OMEXMLService service = factory.getInstance(OMEXMLService.class);
                IMetadata meta = service.createOMEXMLMetadata();
                ImageProcessorReader reader = new ImageProcessorReader();
                reader.setMetadataStore(meta);
                for (String f : main.imagesFiles) {
                    rootName = FilenameUtils.getBaseName(f);
                    reader.setId(f);
                    reader.setSeries(0);
                    int sizeC = reader.getSizeC();
                    int sizeZ = reader.getSizeZ();
                    cal = Image_Utils.findImageCalib(meta);
                    String channelsID = meta.getImageName(0);
                    String[] chs = channelsID.replace("_", "-").split("/");
                    ImporterOptions options = new ImporterOptions();
                    options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                    options.setId(f);
                    options.setSplitChannels(true);
                    options.setZBegin(0, main.removeSlice);
                    if (2 * main.removeSlice < sizeZ)
                        options.setZEnd(0, sizeZ-1  - main.removeSlice);
                    options.setZStep(0, 1);
                    options.setQuiet(true);

                    /*
                    * Open Channel 1 (gene reference)
                    */
                    int channelIndex = ArrayUtils.indexOf(chs, main.channels.get(1)) + 1;
                    String imgChName = main.imagesFolder + File.separatorChar+rootName + "_w" + channelIndex + main.channels.get(1)+ ".TIF";
                    
                    System.out.println("-- Opening gene reference channel : "+ main.channels.get(1));
                    ImagePlus imgGeneRef = IJ.openImage(imgChName);
                    imgGeneRef.setCalibration(cal);
                    
                    /*
                    * Open Channel 3 (gene X)
                    */
                    channelIndex = ArrayUtils.indexOf(chs, main.channels.get(2)) + 1;
                    imgChName = main.imagesFolder + File.separatorChar+rootName + "_w" + channelIndex + main.channels.get(2)+ ".TIF";
                    System.out.println("-- Opening gene X channel : " + main.channels.get(2));
                    ImagePlus imgGeneX = IJ.openImage(imgChName);
                    imgGeneX.setCalibration(cal);

                    Roi roiGeneRef = null, roiGeneX = null;

                    // Background detection methods

                    switch (main.autoBackground) {
                        // from rois
                        case "From rois" :
                            String roiFile = main.imagesFolder+ File.separator + rootName + ".zip";
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
                    channelIndex = ArrayUtils.indexOf(chs, main.channels.get(0)) + 1;
                    imgChName = main.imagesFolder + File.separator + rootName + "_w" + channelIndex+ main.channels.get(0)+ ".TIF";
                    System.out.println("-- Opening Nucleus channel : "+ main.channels.get(0));
                    ImagePlus imgNuc = IJ.openImage(imgChName);
                    imgNuc.setCalibration(cal);
                    imgNuc.deleteRoi();
                    imgNuc.updateAndDraw();
                    // if DeepL segmente nucleus with StarDist else find cells with cellOutliner
                    // else dilate nucleus
                    Objects3DPopulation cellsPop = new Objects3DPopulation();
                    if (main.nucSegMethod.equals("StarDist"))
                        cellsPop = process.stardistNucleiPop(imgNuc);
                    else
                        cellsPop = process.findNucleus(imgNuc);
                    
                    System.out.println(cellsPop.getNbObjects()+"-- Nuclei found");
                    // save random color nucleus popualation
                    process.saveCells(imgNuc, cellsPop, outDirResults, rootName);

                    // Find gene reference dots
                    Objects3DPopulation geneRefDots = new Objects3DPopulation();
                    if (main.geneSegMethod.equals("StarDist"))
                        geneRefDots = process.stardistGenePop(imgGeneRef, null);
                    else
                        geneRefDots = process.findGenePop(imgGeneRef, null);
                    System.out.println("Finding gene "+geneRefDots.getNbObjects()+" reference dots");

                    //Find gene X dots
                    Objects3DPopulation geneXDots = new Objects3DPopulation();
                    if (main.geneSegMethod.equals("StarDist"))
                        geneXDots = process.stardistGenePop(imgGeneX, null);
                    else
                        geneXDots = process.findGenePop(imgGeneX, null);
                    System.out.println("Finding gene "+geneXDots.getNbObjects()+" X dots");

                    
                    // Find cells parameters in geneRef and geneX images
                    ArrayList<Cell> listCells = process.tagsCells(cellsPop, geneRefDots, geneXDots, imgGeneRef, imgGeneX, roiGeneRef, roiGeneX);

                    // write results for each cell population
                    for (int n = 0; n < listCells.size(); n++) {
                        main.output_detail_Analyze.write(rootName+"\t"+listCells.get(n).getIndex()+"\t"+listCells.get(n).getCellVol()+"\t"+listCells.get(n).getzCell()+"\t"+listCells.get(n).getCellGeneRefInt()
                                +"\t"+listCells.get(n).getCellGeneRefBgInt()+"\t"+listCells.get(n).getnbGeneRefDotsCellInt()+"\t"+listCells.get(n).getGeneRefDotsVol()+"\t"+listCells.get(n).getGeneRefDotsInt()
                                +"\t"+listCells.get(n).getnbGeneRefDotsSegInt()+"\t"+listCells.get(n).getCellGeneXInt()+"\t"+listCells.get(n).getCellGeneXBgInt()+"\t"+listCells.get(n).getnbGeneXDotsCellInt()
                                +"\t"+listCells.get(n).getGeneXDotsVol()+"\t"+listCells.get(n).getGeneXDotsInt()+"\t"+listCells.get(n).getnbGeneXDotsSegInt()+"\n");
                        main.output_detail_Analyze.flush();                       

                    }
                    
                    // save dots segmented objects
                    process.saveDotsImage (imgNuc, cellsPop, geneRefDots, geneXDots, outDirResults, rootName);

                    process.closeImages(imgNuc);
                    process.closeImages(imgGeneRef);
                    process.closeImages(imgGeneX);
                }
                if (new File(outDirResults + "detailed_results.xls").exists())
                    main.output_detail_Analyze.close();
                } catch (IOException | DependencyException | ServiceException | FormatException ex) {
                    Logger.getLogger(RNA_Scope_Local.class.getName()).log(Level.SEVERE, null, ex);
                }

            IJ.showStatus("Process done ...");
        }
    
    
}
