package RNA_Scope;


import ij.IJ;
import ij.ImagePlus;
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
import loci.plugins.in.ImporterOptions;
import mcib3d.geom.Objects3DPopulation;
import org.apache.commons.io.FilenameUtils;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.GaussianBlur3D;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.Collections;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DVoxels;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.distanceMap3d.EDT;
import mcib3d.image3d.processing.FastFilters3D;
import mcib3d.image3d.regionGrowing.Watershed3D;


/**
 *
 * @author phm
 */
public class RNA_Scope_Local_Cells implements PlugIn {
    
    private Calibration cal = new Calibration();
    private double minNucVol = 50;
    private double maxNucVol = 1500;
    private int nucDil = 5;
    private RNA_Scope.RNA_Scope_Main main = new RNA_Scope.RNA_Scope_Main();
    private RNA_Scope_Utils.RNA_Scope_Processing process =  new  RNA_Scope_Utils.RNA_Scope_Processing();
    
    /**
     * Find images in folder
     */
    private ArrayList findImages(String imagesFolder, String imageExt) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No Image found in "+imagesFolder);
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            // Find images with extension
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExt))
                images.add(imagesFolder + File.separator + f);
        }
        Collections.sort(images);
        return(images);
    }
    
   /**
     * Save nucleus with random colors
     * @param imgNuc
     * @param cellsPop
     * @param outDirResults
     * @param rootName
     */
    private void saveCells (ImagePlus imgNuc, Objects3DPopulation cellsPop, String outDirResults, String rootName) {
        ImagePlus imgGreyPop = labelPop (cellsPop, imgNuc);
        FileSaver ImgGreyObjectsFile = new FileSaver(imgGreyPop);
        ImgGreyObjectsFile.saveAsTiff(outDirResults + rootName + "_Cells-ColorObjects.tif");
        process.closeImages(imgGreyPop);
    }
    
    
    /**
     * label nucleus population
     */
    private ImagePlus labelPop (Objects3DPopulation cellsPop,  ImagePlus img) {
        //create image objects population
        ImageHandler imgObj = ImageInt.wrap(img).createSameDimensions();
        imgObj.setCalibration(img.getCalibration());
        int color = 9;
        for (int i = 0; i < cellsPop.getNbObjects(); i++) {
            color++;
            Object3D obj = cellsPop.getObject(i);
            obj.draw(imgObj, color);
        } 
        return(imgObj.getImagePlus());
    } 
    
    /**
     * Return dilated object restriced to image borders
     * @param img
     * @param obj
     * @return 
     */
    private Object3DVoxels dilCellObj(ImagePlus img, Object3D obj) {
        Object3D objDil = obj.getDilatedObject((float)(nucDil/cal.pixelWidth), (float)(nucDil/cal.pixelHeight), 
                (float)(nucDil/cal.pixelDepth));
        // check if object go outside image
        if (objDil.getXmin() < 0 || objDil.getXmax() > img.getWidth() || objDil.getYmin() < 0 || objDil.getYmax() > img.getHeight()
                || objDil.getZmin() < 0 || objDil.getZmax() > img.getNSlices()) {
            Object3DVoxels voxObj = new Object3DVoxels(objDil.listVoxels(ImageHandler.wrap(img)));
            return(voxObj);
        }
        else
            return(objDil.getObject3DVoxels());
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
     * Nucleus segmentation 2
     * @param imgNuc
     * @return cellPop
     */
    public Objects3DPopulation find_nucleus2(ImagePlus imgNuc) {
        ImagePlus img = new Duplicator().run(imgNuc);
        ImageStack stack = new ImageStack(img.getWidth(), imgNuc.getHeight());
        IJ.run(img, "Remove Outliers", "block_radius_x=20 block_radius_y=20 standard_deviations=1 stack");

        for (int i = 1; i <= img.getStackSize(); i++) {
            IJ.showStatus("Finding nucleus section "+i+" / "+img.getStackSize());
            img.setZ(i);
            img.updateAndDraw();
            IJ.run(img, "Nuclei Outline", "blur=20 blur2=30 threshold_method=Otsu outlier_radius=0 outlier_threshold=1 max_nucleus_size=100 "
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
        process.closeImages(imgStack);
        imgWater.setCalibration(cal);
        Objects3DPopulation cellPop = new Objects3DPopulation(imgWater);
        cellPop.removeObjectsTouchingBorders(imgWater, false);
        process.closeImages(imgWater);
        process.closeImages(img);
        return(cellPop);
    }
    
    public void findNucleus(ImagePlus imgNuc, String dir, String name) {
        Objects3DPopulation nucPopOrg = new Objects3DPopulation();
        nucPopOrg = find_nucleus2(imgNuc);
        System.out.println("-- Total nucleus Population :"+nucPopOrg.getNbObjects());
        // size filter
        Objects3DPopulation nucPop = new Objects3DPopulation(nucPopOrg.getObjectsWithinVolume(minNucVol, maxNucVol, true));
        int nbNucPop = nucPop.getNbObjects();
        System.out.println("-- Total nucleus Population after size filter: "+ nbNucPop);
        saveCells(imgNuc, nucPop, dir, name+"_nuc");
        
        // create dilated nucleus population
        Objects3DPopulation cellsPop = new Objects3DPopulation();
        for (int o = 0; o < nucPop.getNbObjects(); o++) {
            Object3D obj = nucPop.getObject(o);
            cellsPop.addObject(dilCellObj(imgNuc, obj));
        }
        saveCells(imgNuc, cellsPop, dir, name+"_cells");
    }
    
    
    @Override
    public void run(String arg) {
            try {
                
                String imageDir = IJ.getDirectory("Choose Directory Containing Image Files...");
                if (imageDir == null) {
                    return;
                }

                // Find images with nd extension
                ArrayList<String> imageFile = findImages(imageDir, "nd");
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
                reader.setId(imageFile.get(0));
                // Find calibration

                 
                // read image calibration
                cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
                cal.pixelHeight = cal.pixelWidth;
                if (meta.getPixelsPhysicalSizeZ(0) != null)
                    cal.pixelDepth = meta.getPixelsPhysicalSizeZ(0).value().doubleValue();
                else
                    cal.pixelDepth = 1;
                cal.setUnit("microns");
                System.out.println("x cal = " +cal.pixelWidth+", z cal=" + cal.pixelDepth);

                for (String f : imageFile) {
                    String rootName = FilenameUtils.getBaseName(f);
                    reader.setId(f);
                    reader.setSeries(0);
                    // find channels name
                    String channelsID = meta.getImageName(0);
                    String[] chs = channelsID.replace("_", "-").split("/");
                    
                    ImporterOptions options = new ImporterOptions();
                    options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                    options.setId(f);
                    options.setSplitChannels(true);
                    int sizeZ = reader.getSizeZ();
                    options.setZBegin(0, main.removeSlice);
                    if (2 * main.removeSlice < sizeZ)
                        options.setZEnd(0, sizeZ-1  - main.removeSlice);
                    options.setZStep(0, 1);
                    options.setCBegin(0, 0);
                    options.setCEnd(0, 0);
                    options.setQuiet(true);

                    /*
                    * Open DAPI channel
                    */
                  
                    System.out.println("-- Opening Nucleus channel");
                    ImagePlus imgNuc = BF.openImagePlus(options)[0];

                    findNucleus(imgNuc, outDirResults, rootName);
                    process.closeImages(imgNuc);
                }
                if (new File(outDirResults + "detailed_results.xls").exists())
                    main.output_detail_Analyze.close();
                } catch (IOException | DependencyException | ServiceException | FormatException ex) {
                    Logger.getLogger(RNA_Scope_Local_Cells.class.getName()).log(Level.SEVERE, null, ex);
                }

            IJ.showStatus("Process done ...");
        }
    
    
}
