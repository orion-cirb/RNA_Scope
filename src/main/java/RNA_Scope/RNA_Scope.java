package RNA_Scope;



import RNA_Scope_Utils.RoiBg;
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
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import mcib3d.geom2.Objects3DIntPopulation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;



/**
 *
 * @author phm
 */
public class RNA_Scope implements PlugIn {
    
private final RNA_Scope_Utils.RNA_Scope_Tools tools =  new  RNA_Scope_Utils.RNA_Scope_Tools();
private String imageDir = "";
private String outDirResults = "";
private String rootName = "";
private BufferedWriter results;

    @Override
    public void run(String arg) {
            try {
                imageDir = IJ.getDirectory("Choose directory containing image files...");
                if (imageDir == null) {
                    return;
                }
                // Find images with extension
                String file_ext = tools.findImageType(new File(imageDir));
                // Find images with nd extension
                ArrayList<String> imageFile = tools.findImages(imageDir, file_ext);
                if (imageFile == null) {
                    IJ.showMessage("Error", "No images found with nd extension");
                    return;
                }
                // create output folder
                String outDirResults = imageDir + File.separator+ "Results"+ File.separator;
                File outDir = new File(outDirResults);
                if (!Files.exists(Paths.get(outDirResults))) {
                    outDir.mkdir();
                }
                
                // create OME-XML metadata store of the latest schema version
                ServiceFactory factory;
                factory = new ServiceFactory();
                OMEXMLService service = factory.getInstance(OMEXMLService.class);
                IMetadata meta = service.createOMEXMLMetadata();
                ImageProcessorReader reader = new ImageProcessorReader();
                reader.setMetadataStore(meta);
                // Find channel names , calibration
                reader.setId(imageFile.get(0));
                tools.cal = tools.findImageCalib(meta);
            
                // Find channel names
                String[] channels = tools.findChannels(imageFile.get(0), meta, reader);
                
                // Channels dialog
                String[] chs = tools.dialog(channels);
                if (chs == null) {
                    IJ.showStatus("Plugin cancelled");
                    return;
                }
                // initialize results files
                FileWriter  fwAnalyze_detail = new FileWriter(outDirResults + tools.bgDetection +"_results.xls",false);
                BufferedWriter output_Analyze = new BufferedWriter(fwAnalyze_detail);
                // write results headers
                output_Analyze.write("Image Name\t#Nucleus\tNucleus Vol (µm3)\tNucleus Integrated intensity in gene ref. channel\tMean background intensity in ref. channel\t"
                        + "Total dots gene ref. (based on nucleus intensity)\tDots ref. volume (µm3)\tIntegrated intensity of dots ref. channel\t"
                        + "Total dots gene ref (based on dots seg intensity)\tNucleus Integrated intensity in gene X channel\tMean background intensity in X channel\t"
                        + "Total dots gene X (based on nucleus intensity)\tDots X volume (µm3)\tIntegrated intensity of dots X channel\t"
                        + "Total dots gene X (based on dots seg intensity)\tNucleus Integrated intensity in gene Y channel\tMean background intensity in Y channel\t"
                        + "Total dots gene Y (based on nucleus intensity)\tDots Y volume (µm3)\tIntegrated intensity of dots Y channel\t"
                        + "Total dots gene Y (based on dots seg intensity)\n");
                output_Analyze.flush();
            
                for (String f : imageFile) {
                    rootName = FilenameUtils.getBaseName(f);
                    reader.setId(f);
                
                    ImporterOptions options = new ImporterOptions();
                    options.setId(f);
                    options.setSplitChannels(true);
                    options.setQuiet(true);
                    options.setCrop(true);
                    options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                    
                    
                    // Background detection methods
                    RoiBg roiGeneRef = new RoiBg(null, -1);
                    RoiBg roiGeneX = new RoiBg(null, -1);
                    RoiBg roiGeneY = new RoiBg(null, -1);

                    switch (tools.bgDetection) {
                        // from rois
                        case "From rois" :
                            String roiFile = imageDir + File.separator + rootName + ".zip";
                            if (!new File(roiFile).exists()) {
                                IJ.showStatus("No roi file found !");
                                return;
                            }
                            // Find roi for gene ref and gene X
                            RoiManager rm = new RoiManager(false);
                            rm.runCommand("Open", roiFile);
                            for (int r = 0; r < rm.getCount(); r++) {
                                Roi roi = rm.getRoi(r);
                                switch (roi.getName()) {
                                    case "geneRef" :
                                        roiGeneRef.setRoi(roi);
                                        break;
                                    case "geneX" :
                                        roiGeneX.setRoi(roi);
                                        break;
                                    case "geneY" :
                                        roiGeneY.setRoi(roi);
                                        break;
                                }
                            }
                            break;
                            case "From calibration" :
                                roiGeneRef.setBgInt(tools.calibBgGeneRef);
                                roiGeneX.setBgInt(tools.calibBgGeneX);
                                if (channels.length > 3)
                                    roiGeneY.setBgInt(tools.calibBgGeneY);
                                break;
                    }
                    
                    /*
                    * Open Channel 0 DAPI channel
                    */
                    System.out.println("-- Opening Nucleus channel : "+ tools.channelsName[0]);
                    int indexCh = ArrayUtils.indexOf(channels, chs[0]);
                    ImagePlus imgNuc = BF.openImagePlus(options)[indexCh];
                    // Segmente nucleus with StarDist
                    Objects3DIntPopulation nucleusPop = tools.stardistNucleiPop(imgNuc);
                    System.out.println(nucleusPop.getNbObjects()+" nuclei found after size threshold");
                                                         
                   
                    /*
                    * Open Channel 1 (gene reference)
                    */
                    indexCh = ArrayUtils.indexOf(channels, chs[1]);                    
                    System.out.println("-- Opening gene reference channel : "+ tools.channelsName[1]);
                    ImagePlus imgGeneRef = BF.openImagePlus(options)[indexCh];
                    // automatic search roi from calibration values     
                    if (tools.bgDetection.equals("Auto"))
                        roiGeneRef = tools.findRoiBackgroundAuto(imgGeneRef);
                    // find all genes Ref
                    Objects3DIntPopulation genesRefPop = tools.stardistGenePop(imgGeneRef, null);
                    System.out.println(genesRefPop.getNbObjects()+ " ref genes found");
                    // find genes Ref in nuclei
                    int genesInNuc = tools.findGenesInNucleus(nucleusPop, genesRefPop);
                    System.out.println(genesInNuc+" gene ref found in nuclei");
                    
                    /*
                    * Open Channel 2 (gene X)
                    */
                    indexCh = ArrayUtils.indexOf(channels, chs[2]);                    
                    System.out.println("-- Opening gene X channel : "+ tools.channelsName[2]);
                    ImagePlus imgGeneX = BF.openImagePlus(options)[indexCh];
                    if (tools.bgDetection.equals("Auto"))
                        roiGeneX = tools.findRoiBackgroundAuto(imgGeneX);
                    // find all genes X
                    Objects3DIntPopulation genesXPop = tools.stardistGenePop(imgGeneX, null);
                    System.out.println(genesXPop.getNbObjects()+ " X genes found");
                    // find genes Ref in nuclei
                    genesInNuc = tools.findGenesInNucleus(nucleusPop, genesXPop);
                    System.out.println(genesInNuc+" gene X found in nuclei");
                    
                    /*
                    * Open Channel 3 (gene Y)
                    */
                    ImagePlus imgGeneY = null;
                    Objects3DIntPopulation genesYPop = new Objects3DIntPopulation();
                    if (channels.length > 3) {
                        indexCh = ArrayUtils.indexOf(channels, chs[3]);
                        System.out.println("-- Opening gene Y channel : "+ tools.channelsName[3]);
                        imgGeneY = BF.openImagePlus(options)[indexCh];
                        if (tools.bgDetection.equals("Auto"))
                            roiGeneY = tools.findRoiBackgroundAuto(imgGeneY);
                        // find all genes X
                        genesYPop = tools.stardistGenePop(imgGeneY, null);
                        System.out.println(genesYPop.getNbObjects()+ " Y genes found");
                        // find genes Ref in nrootNameuclei
                        genesInNuc = tools.findGenesInNucleus(nucleusPop, genesYPop);
                        System.out.println(genesInNuc+" gene Y found in nuclei");
                    }
                    
                     
                     // save dots segmented objects
                    tools.saveDotsImage (imgNuc, nucleusPop, genesRefPop, genesXPop, genesYPop, outDirResults, rootName);
                    tools.flush_close(imgNuc);
                    
                    // write results for each cell population
                    tools.writeResults(nucleusPop, genesRefPop, genesXPop, genesYPop, imgGeneRef, imgGeneX, imgGeneY, roiGeneRef, roiGeneX, roiGeneY
                            , rootName, output_Analyze);
                   
                    tools.flush_close(imgGeneRef);
                    tools.flush_close(imgGeneX);
                    tools.flush_close(imgGeneY);
                }
                if (new File(outDirResults + "detailed_results.xls").exists())
                    output_Analyze.close();
                } catch (IOException | DependencyException | ServiceException | FormatException ex) {
                    Logger.getLogger(RNA_Scope.class.getName()).log(Level.SEVERE, null, ex);
                }

            IJ.showStatus("Process done ...");
        }
    
    
}
