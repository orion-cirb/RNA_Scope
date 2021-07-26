/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RNA_Scope;

import RNA_Scope_Utils.OmeroConnect;
import static RNA_Scope_Utils.OmeroConnect.connect;
import static RNA_Scope_Utils.OmeroConnect.findAllImages;
import static RNA_Scope_Utils.OmeroConnect.findDataset;
import static RNA_Scope_Utils.OmeroConnect.findDatasets;
import static RNA_Scope_Utils.OmeroConnect.findProject;
import static RNA_Scope_Utils.OmeroConnect.findUserProjects;
import static RNA_Scope_Utils.OmeroConnect.getUserId;
import ij.IJ;
import ij.measure.Calibration;
import ij.process.AutoThresholder;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.text.NumberFormatter;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.util.ImageProcessorReader;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ImageData;
import omero.gateway.model.ProjectData;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author phm
 */
public class RNA_Scope_JDialog extends javax.swing.JDialog {
    
    
    RNA_Scope_Main rna = null;
    
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    NumberFormatter nff = new NumberFormatter(nf);
    
    // Omero connection
    public static String serverName = "omero.college-de-france.fr";
    public static int serverPort = 4064;
    public static  String userID = "";
    public static String userPass = "";
    private ArrayList<ProjectData> projects = new ArrayList<>();
    private ArrayList<DatasetData> datasets = new ArrayList<>();
    
    // parameters
    
    public static ProjectData selectedProjectData;
    public static DatasetData selectedDatasetData;
    public static ArrayList<ImageData> imageData;
    public static String selectedProject;
    public static String selectedDataset;
    public static boolean connectSuccess = false;
    
    private String[] segMethods = {"Classical","StarDist"};
    private String[] autoThresholdMethods = AutoThresholder.getMethods();
    private String[] autoBackgroundMethods = {"From rois", "Auto", "From calibration"};
    public Calibration cal = new Calibration();
    private List<String> chs = new ArrayList<>();
    private boolean actionListener;

    /**
     * Creates new form RNA_Scope_JDialog
     */
    public RNA_Scope_JDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    //System.out.println(info.getName()+" ");
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        initComponents();
        rna = new RNA_Scope_Main().instance;
    }
    
    private List<String> findChannels(String imagesFolder) throws DependencyException, ServiceException, FormatException, IOException {
        List<String> channels = new ArrayList<>();
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No Image found in "+imagesFolder);
            return null;
        }
        // create OME-XML metadata store of the latest schema version
        ServiceFactory factory;
        factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata meta = service.createOMEXMLMetadata();
        ImageProcessorReader reader = new ImageProcessorReader();
        reader.setMetadataStore(meta);
        if (rna.imagesFiles != null)
            rna.imagesFiles.clear();
        int imageNum = 0;
        for (String f : files) {
            // Find nd or ics files
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals("nd")) {
                imageNum++;
                String imageName = imagesFolder + File.separator + f;
                reader.setId(imageName);
                int sizeZ = reader.getSizeZ();
                if (imageNum == 1) {
                    cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
                    cal.pixelHeight = cal.pixelWidth;
                    if (meta.getPixelsPhysicalSizeZ(0) != null) {
                        cal.pixelDepth = meta.getPixelsPhysicalSizeZ(0).value().doubleValue();
                        jFormattedTextFieldCalibZ.setEnabled(false);
                    }
                    else
                        cal.pixelDepth = 0.5; 
                    cal.setUnit("microns");
                    jFormattedTextFieldCalibX.setValue(cal.pixelWidth);
                    jFormattedTextFieldCalibX.setEnabled(false);
                    jFormattedTextFieldCalibY.setValue(cal.pixelHeight);
                    jFormattedTextFieldCalibY.setEnabled(false);
                    actionListener = false;
                    jFormattedTextFieldCalibZ.setValue(cal.pixelDepth);
                    actionListener = true;
                    System.out.println("x/y cal = " +cal.pixelWidth+", z cal = " + cal.pixelDepth+", stack size = " + sizeZ);
                    String channelsID = meta.getImageName(0);
                    channels = Arrays.asList(channelsID.replace("_", "-").split("/"));
                }
                rna.imagesFiles.add(imageName);
            }
        }
        return(channels);
    }
    
    /**
     * Add channels 
     */
    private void addChannels(List<String> channels){
        if (jComboBoxDAPICh.getItemCount() == 0) {
            for (String ch : channels) {
                jComboBoxDAPICh.addItem(ch);
                jComboBoxGeneRefCh.addItem(ch);
                jComboBoxGeneXCh.addItem(ch);
            }
        }
    }
            

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPaneRNA_Scope = new javax.swing.JTabbedPane();
        jPanelLocal = new javax.swing.JPanel();
        jLabelImagesFolder = new javax.swing.JLabel();
        jTextFieldImagesFolder = new javax.swing.JTextField();
        jButtonBrowse = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jPanelOmero = new javax.swing.JPanel();
        jLabelUser = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaImages = new javax.swing.JTextArea();
        jLabelPassword = new javax.swing.JLabel();
        jPasswordField = new javax.swing.JPasswordField();
        jButtonConnect = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel8 = new javax.swing.JLabel();
        jComboBoxProjects = new javax.swing.JComboBox<>();
        jLabelProjects = new javax.swing.JLabel();
        jLabelDatasets = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldServerName = new javax.swing.JTextField();
        jLabelPort = new javax.swing.JLabel();
        jTextFieldPort = new javax.swing.JTextField();
        jComboBoxDatasets = new javax.swing.JComboBox<>();
        jTextFieldUserID = new javax.swing.JTextField();
        jLabelImages = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanelParameters = new javax.swing.JPanel();
        jFormattedTextFieldNucDil = new javax.swing.JFormattedTextField();
        jFormattedTextFieldSecToRemove = new javax.swing.JFormattedTextField();
        jLabelSecToRemove = new javax.swing.JLabel();
        jLabelBg = new javax.swing.JLabel();
        jLabelGeneRefSingleDotInt = new javax.swing.JLabel();
        jFormattedTextFieldGeneRefSingleDotInt = new javax.swing.JFormattedTextField();
        jLabelNucleus = new javax.swing.JLabel();
        jLabelGeneXSingleDotInt = new javax.swing.JLabel();
        jFormattedTextFieldGeneXSingleDotInt = new javax.swing.JFormattedTextField();
        jLabelDAPICh = new javax.swing.JLabel();
        jLabelSingleDotsCalib = new javax.swing.JLabel();
        jComboBoxDAPICh = new javax.swing.JComboBox();
        jLabelBgMethod = new javax.swing.JLabel();
        jLabelGeneRefCh = new javax.swing.JLabel();
        jComboBoxBgMethod = new javax.swing.JComboBox(autoBackgroundMethods);
        jComboBoxGeneRefCh = new javax.swing.JComboBox();
        jLabelBgRoiSize = new javax.swing.JLabel();
        jLabelGeneXCh = new javax.swing.JLabel();
        jFormattedTextFieldBgRoiSize = new javax.swing.JFormattedTextField();
        jComboBoxGeneXCh = new javax.swing.JComboBox();
        jLabelCalibBgGeneRef = new javax.swing.JLabel();
        jLabelChannels = new javax.swing.JLabel();
        jFormattedTextFieldCalibBgGeneRef = new javax.swing.JFormattedTextField();
        jLabelThMethod = new javax.swing.JLabel();
        jFormattedTextFieldCalibBgGeneX = new javax.swing.JFormattedTextField();
        jComboBoxThMethod = new javax.swing.JComboBox(autoThresholdMethods);
        jLabelMinVol = new javax.swing.JLabel();
        jLabelCalibBgGeneX = new javax.swing.JLabel();
        jLabelBgCalib = new javax.swing.JLabel();
        jFormattedTextFieldMinVol = new javax.swing.JFormattedTextField();
        jLabelMaxVol = new javax.swing.JLabel();
        jFormattedTextFieldMaxVol = new javax.swing.JFormattedTextField();
        jLabelNucDil = new javax.swing.JLabel();
        jLabelCalibX = new javax.swing.JLabel();
        jFormattedTextFieldCalibX = new javax.swing.JFormattedTextField();
        jLabelCalibY = new javax.swing.JLabel();
        jFormattedTextFieldCalibY = new javax.swing.JFormattedTextField();
        jLabelCalibZ = new javax.swing.JLabel();
        jFormattedTextFieldCalibZ = new javax.swing.JFormattedTextField();
        jLabelCSpatialCalib = new javax.swing.JLabel();
        jCheckBoxNumberNucleus = new javax.swing.JCheckBox();
        jLabelSegMethod = new javax.swing.JLabel();
        jComboBoxSegMethod = new javax.swing.JComboBox(segMethods);
        jButtonOk = new javax.swing.JToggleButton();
        jButtonCancel = new javax.swing.JToggleButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Parameters");
        setResizable(false);

        jTabbedPaneRNA_Scope.setToolTipText("");

        jLabelImagesFolder.setText("Images folder : ");

        jButtonBrowse.setText("Browse");
        jButtonBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonBrowseActionPerformed(evt);
            }
        });

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Orion_icon.png"))); // NOI18N

        javax.swing.GroupLayout jPanelLocalLayout = new javax.swing.GroupLayout(jPanelLocal);
        jPanelLocal.setLayout(jPanelLocalLayout);
        jPanelLocalLayout.setHorizontalGroup(
            jPanelLocalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLocalLayout.createSequentialGroup()
                .addGroup(jPanelLocalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelLocalLayout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(jLabelImagesFolder)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldImagesFolder, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(57, 57, 57)
                        .addComponent(jButtonBrowse))
                    .addGroup(jPanelLocalLayout.createSequentialGroup()
                        .addGap(290, 290, 290)
                        .addComponent(jLabel2)))
                .addContainerGap(237, Short.MAX_VALUE))
        );
        jPanelLocalLayout.setVerticalGroup(
            jPanelLocalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLocalLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(46, 46, 46)
                .addGroup(jPanelLocalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelImagesFolder)
                    .addComponent(jTextFieldImagesFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonBrowse))
                .addContainerGap(513, Short.MAX_VALUE))
        );

        jTabbedPaneRNA_Scope.addTab("Local images", jPanelLocal);

        jLabelUser.setText("user ID : ");

        jTextAreaImages.setEditable(false);
        jTextAreaImages.setColumns(20);
        jTextAreaImages.setLineWrap(true);
        jTextAreaImages.setRows(5);
        jScrollPane1.setViewportView(jTextAreaImages);

        jLabelPassword.setText("Password : ");

        jPasswordField.setText("");
        jPasswordField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jPasswordFieldFocusLost(evt);
            }
        });
        jPasswordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPasswordFieldActionPerformed(evt);
            }
        });

        jButtonConnect.setText("Connect");
        jButtonConnect.setEnabled(false);
        jButtonConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConnectActionPerformed(evt);
            }
        });

        jLabel8.setText("OMERO Database");

        jComboBoxProjects.setEnabled(false);
        jComboBoxProjects.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxProjectsActionPerformed(evt);
            }
        });

        jLabelProjects.setText("Projects : ");

        jLabelDatasets.setText("Datasets : ");

        jLabel1.setText("Server name : ");

        jTextFieldServerName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldServerNameActionPerformed(evt);
            }
        });
        jTextFieldServerName.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jTextFieldServerNamePropertyChange(evt);
            }
        });

        jLabelPort.setText("Port : ");

        jTextFieldPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPortActionPerformed(evt);
            }
        });
        jTextFieldPort.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jTextFieldPortPropertyChange(evt);
            }
        });

        jComboBoxDatasets.setEnabled(false);
        jComboBoxDatasets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDatasetsActionPerformed(evt);
            }
        });

        jTextFieldUserID.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextFieldUserIDFocusLost(evt);
            }
        });
        jTextFieldUserID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldUserIDActionPerformed(evt);
            }
        });

        jLabelImages.setText("Images :");

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Orion_icon.png"))); // NOI18N

        javax.swing.GroupLayout jPanelOmeroLayout = new javax.swing.GroupLayout(jPanelOmero);
        jPanelOmero.setLayout(jPanelOmeroLayout);
        jPanelOmeroLayout.setHorizontalGroup(
            jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelOmeroLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jButtonConnect)
                .addGap(338, 338, 338))
            .addGroup(jPanelOmeroLayout.createSequentialGroup()
                .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelOmeroLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelPort, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabelUser, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabelPassword, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldServerName, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldUserID, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanelOmeroLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelImages)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 578, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanelOmeroLayout.createSequentialGroup()
                        .addGap(212, 212, 212)
                        .addComponent(jLabel8))
                    .addGroup(jPanelOmeroLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelDatasets, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabelProjects, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jComboBoxProjects, javax.swing.GroupLayout.Alignment.LEADING, 0, 327, Short.MAX_VALUE)
                            .addComponent(jComboBoxDatasets, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanelOmeroLayout.createSequentialGroup()
                        .addGap(72, 72, 72)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 582, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelOmeroLayout.createSequentialGroup()
                        .addGap(322, 322, 322)
                        .addComponent(jLabel3)))
                .addContainerGap(152, Short.MAX_VALUE))
        );
        jPanelOmeroLayout.setVerticalGroup(
            jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOmeroLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addGap(38, 38, 38)
                .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldServerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelPort))
                .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelOmeroLayout.createSequentialGroup()
                        .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldUserID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelUser, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jPasswordField)
                            .addComponent(jLabelPassword, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(62, 62, 62))
                    .addGroup(jPanelOmeroLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonConnect)
                        .addGap(28, 28, 28)))
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel8)
                .addGap(26, 26, 26)
                .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelProjects)
                    .addComponent(jComboBoxProjects, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanelOmeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelDatasets)
                    .addComponent(jComboBoxDatasets, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelImages)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(45, 45, 45))
        );

        jTextFieldServerName.setText("omero.college-de-france.fr");
        jTextFieldPort.setText("4064");

        jTabbedPaneRNA_Scope.addTab("Omero server", jPanelOmero);

        jFormattedTextFieldNucDil.setForeground(java.awt.Color.black);
        jFormattedTextFieldNucDil.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        jFormattedTextFieldNucDil.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldNucDilActionPerformed(evt);
            }
        });
        jFormattedTextFieldNucDil.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldNucDilPropertyChange(evt);
            }
        });

        jFormattedTextFieldSecToRemove.setForeground(java.awt.Color.black);
        jFormattedTextFieldSecToRemove.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        jFormattedTextFieldSecToRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldSecToRemoveActionPerformed(evt);
            }
        });
        jFormattedTextFieldSecToRemove.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldSecToRemovePropertyChange(evt);
            }
        });

        jLabelSecToRemove.setText("Section to remove : ");

        jLabelBg.setFont(new java.awt.Font("Cantarell", 3, 15)); // NOI18N
        jLabelBg.setText("Background detection");

        jLabelGeneRefSingleDotInt.setText("Gene ref. single dot intensity : ");

        jFormattedTextFieldGeneRefSingleDotInt.setForeground(java.awt.Color.black);
        jFormattedTextFieldGeneRefSingleDotInt.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        jFormattedTextFieldGeneRefSingleDotInt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldGeneRefSingleDotIntActionPerformed(evt);
            }
        });
        jFormattedTextFieldGeneRefSingleDotInt.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldGeneRefSingleDotIntPropertyChange(evt);
            }
        });

        jLabelNucleus.setFont(new java.awt.Font("Cantarell", 3, 15)); // NOI18N
        jLabelNucleus.setText("Nucleus parameters");

        jLabelGeneXSingleDotInt.setText("Gene X single dot intensity : ");

        jFormattedTextFieldGeneXSingleDotInt.setForeground(java.awt.Color.black);
        jFormattedTextFieldGeneXSingleDotInt.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        jFormattedTextFieldGeneXSingleDotInt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldGeneXSingleDotIntActionPerformed(evt);
            }
        });
        jFormattedTextFieldGeneXSingleDotInt.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldGeneXSingleDotIntPropertyChange(evt);
            }
        });

        jLabelDAPICh.setText("DAPI :");

        jLabelSingleDotsCalib.setFont(new java.awt.Font("Ubuntu", 3, 15)); // NOI18N
        jLabelSingleDotsCalib.setText("Single dot calibration");

        jComboBoxDAPICh.setForeground(new java.awt.Color(0, 0, 0));
        jComboBoxDAPICh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDAPIChActionPerformed(evt);
            }
        });

        jLabelBgMethod.setText("Background method : ");

        jLabelGeneRefCh.setText("Gene ref. :");

        jComboBoxBgMethod.setForeground(java.awt.Color.black);
        jComboBoxBgMethod.setToolTipText("Select background method");
        jComboBoxBgMethod.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxBgMethodItemStateChanged(evt);
            }
        });
        jComboBoxBgMethod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxBgMethodActionPerformed(evt);
            }
        });

        jComboBoxGeneRefCh.setForeground(java.awt.Color.black);
        jComboBoxGeneRefCh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxGeneRefChActionPerformed(evt);
            }
        });

        jLabelBgRoiSize.setText("Size of background box size : ");

        jLabelGeneXCh.setText("Gene X :");

        jFormattedTextFieldBgRoiSize.setForeground(java.awt.Color.black);
        jFormattedTextFieldBgRoiSize.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        jFormattedTextFieldBgRoiSize.setEnabled(false);
        jFormattedTextFieldBgRoiSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldBgRoiSizeActionPerformed(evt);
            }
        });
        jFormattedTextFieldBgRoiSize.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldBgRoiSizePropertyChange(evt);
            }
        });

        jComboBoxGeneXCh.setForeground(java.awt.Color.black);
        jComboBoxGeneXCh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxGeneXChActionPerformed(evt);
            }
        });

        jLabelCalibBgGeneRef.setText("Gene reference intensity :");

        jLabelChannels.setFont(new java.awt.Font("Cantarell", 3, 15)); // NOI18N
        jLabelChannels.setText("Channels parameters");

        jFormattedTextFieldCalibBgGeneRef.setForeground(java.awt.Color.black);
        jFormattedTextFieldCalibBgGeneRef.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        jFormattedTextFieldCalibBgGeneRef.setEnabled(false);
        jFormattedTextFieldCalibBgGeneRef.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldCalibBgGeneRefActionPerformed(evt);
            }
        });
        jFormattedTextFieldCalibBgGeneRef.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldCalibBgGeneRefPropertyChange(evt);
            }
        });

        jLabelThMethod.setText("Threshold method : ");

        jFormattedTextFieldCalibBgGeneX.setForeground(java.awt.Color.black);
        jFormattedTextFieldCalibBgGeneX.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        jFormattedTextFieldCalibBgGeneX.setEnabled(false);
        jFormattedTextFieldCalibBgGeneX.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldCalibBgGeneXActionPerformed(evt);
            }
        });
        jFormattedTextFieldCalibBgGeneX.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldCalibBgGeneXPropertyChange(evt);
            }
        });

        jComboBoxThMethod.setForeground(java.awt.Color.black);
        jComboBoxThMethod.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxThMethodItemStateChanged(evt);
            }
        });
        jComboBoxThMethod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxThMethodActionPerformed(evt);
            }
        });

        jLabelMinVol.setText("Min Volume : ");

        jLabelCalibBgGeneX.setText("Gene X  intensity  :");

        jLabelBgCalib.setFont(new java.awt.Font("Ubuntu", 3, 15)); // NOI18N
        jLabelBgCalib.setText("Background intensity from calibration");

        jFormattedTextFieldMinVol.setForeground(java.awt.Color.black);
        jFormattedTextFieldMinVol.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        jFormattedTextFieldMinVol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldMinVolActionPerformed(evt);
            }
        });
        jFormattedTextFieldMinVol.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldMinVolPropertyChange(evt);
            }
        });

        jLabelMaxVol.setText("Max Volume : ");

        jFormattedTextFieldMaxVol.setForeground(java.awt.Color.black);
        jFormattedTextFieldMaxVol.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        jFormattedTextFieldMaxVol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldMaxVolActionPerformed(evt);
            }
        });
        jFormattedTextFieldMaxVol.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldMaxVolPropertyChange(evt);
            }
        });

        jLabelNucDil.setText("Nucleus dilatation : ");

        jLabelCalibX.setText("size X  : ");

        jFormattedTextFieldCalibX.setForeground(java.awt.Color.black);
        jFormattedTextFieldCalibX.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getNumberInstance())));

        jLabelCalibY.setText("size Y  : ");

        jFormattedTextFieldCalibY.setForeground(java.awt.Color.black);
        jFormattedTextFieldCalibY.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getNumberInstance())));

        jLabelCalibZ.setText("size Z : ");

        jFormattedTextFieldCalibZ.setForeground(java.awt.Color.black);
        jFormattedTextFieldCalibZ.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getNumberInstance())));
        jFormattedTextFieldCalibZ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextFieldCalibZActionPerformed(evt);
            }
        });
        jFormattedTextFieldCalibZ.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFormattedTextFieldCalibZPropertyChange(evt);
            }
        });

        jLabelCSpatialCalib.setFont(new java.awt.Font("Cantarell", 3, 15)); // NOI18N
        jLabelCSpatialCalib.setText("Spatial calibration");

        jCheckBoxNumberNucleus.setSelected(true);
        jCheckBoxNumberNucleus.setText("Numbered nucleus :");
        jCheckBoxNumberNucleus.setToolTipText("Add number to nucleus objects image");
        jCheckBoxNumberNucleus.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jCheckBoxNumberNucleus.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxNumberNucleusItemStateChanged(evt);
            }
        });
        jCheckBoxNumberNucleus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxNumberNucleusActionPerformed(evt);
            }
        });

        jLabelSegMethod.setText("Detection :");

        jComboBoxSegMethod.setForeground(java.awt.Color.black);
        jComboBoxSegMethod.setToolTipText("Select nucleus detection Classical or StarDist method");
        jComboBoxSegMethod.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxSegMethodItemStateChanged(evt);
            }
        });
        jComboBoxSegMethod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxSegMethodActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelParametersLayout = new javax.swing.GroupLayout(jPanelParameters);
        jPanelParameters.setLayout(jPanelParametersLayout);
        jPanelParametersLayout.setHorizontalGroup(
            jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelParametersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelParametersLayout.createSequentialGroup()
                        .addComponent(jLabelChannels)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelSingleDotsCalib)
                            .addGroup(jPanelParametersLayout.createSequentialGroup()
                                .addGap(17, 17, 17)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelParametersLayout.createSequentialGroup()
                                        .addComponent(jLabelGeneRefSingleDotInt)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jFormattedTextFieldGeneRefSingleDotInt, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelParametersLayout.createSequentialGroup()
                                        .addComponent(jLabelGeneXSingleDotInt)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jFormattedTextFieldGeneXSingleDotInt, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabelBgCalib)
                            .addGroup(jPanelParametersLayout.createSequentialGroup()
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelCalibBgGeneRef, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabelCalibBgGeneX, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jFormattedTextFieldCalibBgGeneRef, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jFormattedTextFieldCalibBgGeneX, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(62, 62, 62))
                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelParametersLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                                        .addComponent(jLabelCalibX)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jFormattedTextFieldCalibX, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                                        .addComponent(jLabelCalibY)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jFormattedTextFieldCalibY, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                                        .addComponent(jLabelCalibZ)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jFormattedTextFieldCalibZ, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addComponent(jLabelCSpatialCalib)
                            .addGroup(jPanelParametersLayout.createSequentialGroup()
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelDAPICh, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabelGeneRefCh, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabelGeneXCh, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jComboBoxDAPICh, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jComboBoxGeneRefCh, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jComboBoxGeneXCh, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelParametersLayout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabelBgMethod)
                                    .addComponent(jLabelBgRoiSize))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jComboBoxBgMethod, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jFormattedTextFieldBgRoiSize, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanelParametersLayout.createSequentialGroup()
                                .addGap(54, 54, 54)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelParametersLayout.createSequentialGroup()
                                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabelSegMethod, javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabelThMethod, javax.swing.GroupLayout.Alignment.TRAILING))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jFormattedTextFieldMinVol, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jFormattedTextFieldMaxVol, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jFormattedTextFieldNucDil, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jFormattedTextFieldSecToRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jComboBoxThMethod, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jComboBoxSegMethod, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                                        .addGap(33, 33, 33)
                                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabelBg)
                                            .addComponent(jCheckBoxNumberNucleus))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelParametersLayout.createSequentialGroup()
                                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabelSecToRemove, javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabelNucDil, javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabelMaxVol, javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabelMinVol, javax.swing.GroupLayout.Alignment.TRAILING))
                                        .addGap(152, 152, 152)))))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(jPanelParametersLayout.createSequentialGroup()
                .addGap(389, 389, 389)
                .addComponent(jLabelNucleus)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelParametersLayout.setVerticalGroup(
            jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelParametersLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelNucleus)
                    .addComponent(jLabelChannels))
                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jComboBoxSegMethod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelSegMethod))
                        .addGap(7, 7, 7)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelThMethod)
                            .addComponent(jComboBoxThMethod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelMinVol)
                            .addComponent(jFormattedTextFieldMinVol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelMaxVol)
                            .addComponent(jFormattedTextFieldMaxVol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelDAPICh)
                            .addComponent(jComboBoxDAPICh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelGeneRefCh)
                            .addComponent(jComboBoxGeneRefCh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelGeneXCh)
                            .addComponent(jComboBoxGeneXCh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelNucDil)
                    .addComponent(jFormattedTextFieldNucDil, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelSecToRemove)
                    .addComponent(jFormattedTextFieldSecToRemove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxNumberNucleus)
                .addGap(74, 74, 74)
                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                        .addComponent(jLabelBg, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelBgMethod)
                            .addComponent(jComboBoxBgMethod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jFormattedTextFieldBgRoiSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelBgRoiSize))
                        .addGap(56, 56, 56)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelParametersLayout.createSequentialGroup()
                                .addComponent(jLabelSingleDotsCalib)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelGeneRefSingleDotInt)
                                    .addComponent(jFormattedTextFieldGeneRefSingleDotInt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jFormattedTextFieldGeneXSingleDotInt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabelGeneXSingleDotInt)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelParametersLayout.createSequentialGroup()
                                .addComponent(jLabelBgCalib)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jFormattedTextFieldCalibBgGeneRef, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabelCalibBgGeneRef))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jFormattedTextFieldCalibBgGeneX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabelCalibBgGeneX)))))
                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                        .addComponent(jLabelCSpatialCalib)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelCalibX)
                            .addComponent(jFormattedTextFieldCalibX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelCalibY)
                            .addComponent(jFormattedTextFieldCalibY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelCalibZ)
                            .addComponent(jFormattedTextFieldCalibZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        jFormattedTextFieldNucDil.setValue(rna.nucDil);
        jFormattedTextFieldSecToRemove.setValue(rna.removeSlice);
        jFormattedTextFieldGeneRefSingleDotInt.setValue(rna.singleDotIntGeneRef);
        jFormattedTextFieldGeneXSingleDotInt.setValue(rna.singleDotIntGeneX);
        jComboBoxBgMethod.setSelectedIndex(0);
        jFormattedTextFieldBgRoiSize.setValue(rna.roiBgSize);
        jFormattedTextFieldCalibBgGeneRef.setValue(rna.calibBgGeneRef);
        jFormattedTextFieldCalibBgGeneX.setValue(rna.calibBgGeneX);
        jComboBoxThMethod.setSelectedIndex(11);
        jFormattedTextFieldMinVol.setValue(rna.minNucVol);
        jFormattedTextFieldMaxVol.setValue(rna.maxNucVol);
        jComboBoxBgMethod.setSelectedIndex(1);

        jTabbedPaneRNA_Scope.addTab("Parameters", jPanelParameters);

        jButtonOk.setText("Ok");
        jButtonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOkActionPerformed(evt);
            }
        });

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPaneRNA_Scope)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButtonCancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonOk)
                        .addGap(21, 21, 21))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPaneRNA_Scope)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonOk)
                    .addComponent(jButtonCancel))
                .addGap(26, 26, 26))
        );

        jTabbedPaneRNA_Scope.getAccessibleContext().setAccessibleName("Images parameters");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonBrowseActionPerformed
        // TODO add your handling code here:
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("~/"));
        fileChooser.setDialogTitle("Choose image directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            rna.imagesFolder = fileChooser.getSelectedFile().getAbsolutePath();
            jTextFieldImagesFolder.setText(rna.imagesFolder);
            try {
                chs = findChannels(rna.imagesFolder);
            } catch (DependencyException | ServiceException | FormatException | IOException ex) {
                Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
            actionListener = false;
            addChannels(chs);
            actionListener = true;
            jComboBoxDAPICh.setSelectedIndex(0);
            jComboBoxGeneRefCh.setSelectedIndex(1);
            jComboBoxGeneXCh.setSelectedIndex(2);
        }
        if (rna.imagesFolder != null) {
            rna.localImages = true;
            jButtonOk.setEnabled(true);
        }
    }//GEN-LAST:event_jButtonBrowseActionPerformed

    private void jPasswordFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jPasswordFieldFocusLost
        // TODO add your handling code here:
        userPass = new String(jPasswordField.getPassword());
        if (!serverName.isEmpty() && serverPort != 0 && !userID.isEmpty() && !userPass.isEmpty())
        jButtonConnect.setEnabled(true);
    }//GEN-LAST:event_jPasswordFieldFocusLost

    private void jPasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPasswordFieldActionPerformed
        // TODO add your handling code here:
        userPass = new String(jPasswordField.getPassword());
        if (!serverName.isEmpty() && serverPort != 0 && !userID.isEmpty() && !userPass.isEmpty())
        jButtonConnect.setEnabled(true);
    }//GEN-LAST:event_jPasswordFieldActionPerformed

    private void jButtonConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonConnectActionPerformed
        // TODO add your handling code here:
        if (serverName.isEmpty() && serverPort == 0 && userID.isEmpty() && userPass.isEmpty()) {
            IJ.showMessage("Error", "Missing parameter(s) to connect to server !!!");
        }
        else {
            try {
                OmeroConnect connect = new OmeroConnect();
                connectSuccess = connect(serverName, serverPort, userID, userPass);
            } catch (Exception ex) {
                //Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(Level.SEVERE, null, ex);
                IJ.showMessage("Error", "Wrong user / password !!!");
            }
            if (connectSuccess) {
                jButtonConnect.setEnabled(false);
                try {
                    projects = findUserProjects(getUserId(userID));
                    if (projects.isEmpty())
                    IJ.showMessage("Error", "No project found for user " + userID);
                    else {
                        if (jComboBoxProjects.getItemCount() > 0)
                        jComboBoxProjects.removeAllItems();
                        for (ProjectData projectData : projects) {
                            jComboBoxProjects.addItem(projectData.getName());
                        }
                        jComboBoxProjects.setEnabled(true);
                        jComboBoxProjects.setSelectedIndex(0);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//GEN-LAST:event_jButtonConnectActionPerformed

    private void jComboBoxProjectsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxProjectsActionPerformed
        try {
            // TODO add your handling code here:
            if (jComboBoxProjects.getItemCount() > 0) {
                selectedProject = jComboBoxProjects.getSelectedItem().toString();
                selectedProjectData = findProject(selectedProject, true);
                datasets = findDatasets(selectedProjectData);
                if (datasets.isEmpty()) {
                    //                    IJ.showMessage("Error", "No dataset found for project " + selectedProject);
                    jComboBoxDatasets.removeAllItems();
                    jTextAreaImages.setText("");
                }
                else {
                    if (jComboBoxDatasets.getItemCount() > 0) {
                        jComboBoxDatasets.removeAllItems();
                        jTextAreaImages.setText("");
                    }
                    for (DatasetData datasetData : datasets)
                    jComboBoxDatasets.addItem(datasetData.getName());
                    jComboBoxDatasets.setEnabled(true);
                    jComboBoxDatasets.setSelectedIndex(0);
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jComboBoxProjectsActionPerformed

    private void jTextFieldServerNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldServerNameActionPerformed
        // TODO add your handling code here:
        serverName = jTextFieldServerName.getText();
        if (!serverName.isEmpty() && serverPort != 0 && !userID.isEmpty() && !userPass.isEmpty())
        jButtonConnect.setEnabled(true);
    }//GEN-LAST:event_jTextFieldServerNameActionPerformed

    private void jTextFieldServerNamePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jTextFieldServerNamePropertyChange
        // TODO add your handling code here:
        serverName = jTextFieldServerName.getText();
        if (!serverName.isEmpty() && serverPort != 0 && !userID.isEmpty() && !userPass.isEmpty())
        jButtonConnect.setEnabled(true);
    }//GEN-LAST:event_jTextFieldServerNamePropertyChange

    private void jTextFieldPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPortActionPerformed
        // TODO add your handling code here:
        serverPort = Integer.parseInt(jTextFieldPort.getText());
        if (!serverName.isEmpty() && serverPort != 0 && !userID.isEmpty() && !userPass.isEmpty())
        jButtonConnect.setEnabled(true);
    }//GEN-LAST:event_jTextFieldPortActionPerformed

    private void jTextFieldPortPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jTextFieldPortPropertyChange
        // TODO add your handling code here:
        serverPort = Integer.parseInt(jTextFieldPort.getText());
        if (!serverName.isEmpty() && serverPort != 0 && !userID.isEmpty() && !userPass.isEmpty())
        jButtonConnect.setEnabled(true);
    }//GEN-LAST:event_jTextFieldPortPropertyChange

    private void jComboBoxDatasetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDatasetsActionPerformed
        // TODO add your handling code here:

        try {
            if (jComboBoxDatasets.getItemCount() > 0) {
                selectedDataset = jComboBoxDatasets.getSelectedItem().toString();
                selectedDatasetData = findDataset(selectedDataset, selectedProjectData, true);
                imageData = findAllImages(selectedDatasetData);
                if (imageData.isEmpty())
                IJ.showMessage("Error", "No image found in dataset " + selectedDataset);
                else {
                    IJ.showStatus(imageData.size() + " images found in datatset " + selectedDataset);
                    jTextAreaImages.setText("");
                    for(ImageData images : imageData)
                    jTextAreaImages.append(images.getName()+"\n");
                    jButtonOk.setEnabled(true);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jComboBoxDatasetsActionPerformed

    private void jTextFieldUserIDFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldUserIDFocusLost
        // TODO add your handling code here:
        userID = jTextFieldUserID.getText();
        if (!serverName.isEmpty() && serverPort != 0 && !userID.isEmpty() && !userPass.isEmpty())
        jButtonConnect.setEnabled(true);
    }//GEN-LAST:event_jTextFieldUserIDFocusLost

    private void jTextFieldUserIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldUserIDActionPerformed
        // TODO add your handling code here:
        userID = jTextFieldUserID.getText();
        if (!serverName.isEmpty() && serverPort != 0 && !userID.isEmpty() && !userPass.isEmpty())
        jButtonConnect.setEnabled(true);
    }//GEN-LAST:event_jTextFieldUserIDActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        // TODO add your handling code here:
        this.dispose();
        rna.channels = null;
        rna.dialogCancel = true;
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jButtonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOkActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_jButtonOkActionPerformed

    private void jComboBoxSegMethodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxSegMethodActionPerformed
        // TODO add your handling code here:
        rna.segMethod = jComboBoxSegMethod.getSelectedItem().toString();
        switch (jComboBoxSegMethod.getSelectedIndex()) {
            case 0 :
                jComboBoxThMethod.setEnabled(true);
                jFormattedTextFieldMinVol.setEnabled(true);
                jFormattedTextFieldMaxVol.setEnabled(true);
                jFormattedTextFieldNucDil.setEnabled(true);
            break;
            case 1 :
                jComboBoxThMethod.setEnabled(false);
                jFormattedTextFieldMinVol.setEnabled(false);
                jFormattedTextFieldMaxVol.setEnabled(false);
                jFormattedTextFieldNucDil.setEnabled(false);
            break;
        }
    }//GEN-LAST:event_jComboBoxSegMethodActionPerformed

    private void jComboBoxSegMethodItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxSegMethodItemStateChanged
        // TODO add your handling code here:
        rna.segMethod = jComboBoxSegMethod.getSelectedItem().toString();
        switch (jComboBoxSegMethod.getSelectedIndex()) {
            case 0 :
                jComboBoxThMethod.setEnabled(true);
                jFormattedTextFieldMinVol.setEnabled(true);
                jFormattedTextFieldMaxVol.setEnabled(true);
                jFormattedTextFieldNucDil.setEnabled(true);
            break;
            case 1 :
                jComboBoxThMethod.setEnabled(false);
                jFormattedTextFieldMinVol.setEnabled(false);
                jFormattedTextFieldMaxVol.setEnabled(false);
                jFormattedTextFieldNucDil.setEnabled(false);
            break;
        }
    }//GEN-LAST:event_jComboBoxSegMethodItemStateChanged

    private void jCheckBoxNumberNucleusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxNumberNucleusActionPerformed
        // TODO add your handling code here:
        rna.nucNumber = jCheckBoxNumberNucleus.isSelected();
    }//GEN-LAST:event_jCheckBoxNumberNucleusActionPerformed

    private void jCheckBoxNumberNucleusItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxNumberNucleusItemStateChanged
        // TODO add your handling code here:
        rna.nucNumber = jCheckBoxNumberNucleus.isSelected();
    }//GEN-LAST:event_jCheckBoxNumberNucleusItemStateChanged

    private void jFormattedTextFieldCalibZPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldCalibZPropertyChange
        // TODO add your handling code here:
        if (actionListener)
        cal.pixelDepth = ((Number)jFormattedTextFieldCalibZ.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldCalibZPropertyChange

    private void jFormattedTextFieldCalibZActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldCalibZActionPerformed
        // TODO add your handling code here:
        if (actionListener)
        cal.pixelDepth = ((Number)jFormattedTextFieldCalibZ.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldCalibZActionPerformed

    private void jFormattedTextFieldMaxVolPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldMaxVolPropertyChange
        // TODO add your handling code here:
        rna.maxNucVol = ((Number)jFormattedTextFieldMaxVol.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldMaxVolPropertyChange

    private void jFormattedTextFieldMaxVolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldMaxVolActionPerformed
        // TODO add your handling code here:
        rna.maxNucVol = ((Number)jFormattedTextFieldMaxVol.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldMaxVolActionPerformed

    private void jFormattedTextFieldMinVolPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldMinVolPropertyChange
        // TODO add your handling code here:
        rna.minNucVol = ((Number)jFormattedTextFieldMinVol.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldMinVolPropertyChange

    private void jFormattedTextFieldMinVolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldMinVolActionPerformed
        // TODO add your handling code here:
        rna.minNucVol = ((Number)jFormattedTextFieldMinVol.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldMinVolActionPerformed

    private void jComboBoxThMethodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxThMethodActionPerformed
        // TODO add your handling code here:
        rna.thMethod = jComboBoxThMethod.getSelectedItem().toString();
    }//GEN-LAST:event_jComboBoxThMethodActionPerformed

    private void jComboBoxThMethodItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxThMethodItemStateChanged
        // TODO add your handling code here:
        rna.thMethod = jComboBoxThMethod.getSelectedItem().toString();
    }//GEN-LAST:event_jComboBoxThMethodItemStateChanged

    private void jFormattedTextFieldCalibBgGeneXPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldCalibBgGeneXPropertyChange
        // TODO add your handling code here:
        rna.calibBgGeneX = ((Number)jFormattedTextFieldCalibBgGeneX.getValue()).doubleValue();
    }//GEN-LAST:event_jFormattedTextFieldCalibBgGeneXPropertyChange

    private void jFormattedTextFieldCalibBgGeneXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldCalibBgGeneXActionPerformed
        // TODO add your handling code here:
        rna.calibBgGeneX = ((Number)jFormattedTextFieldCalibBgGeneX.getValue()).doubleValue();
    }//GEN-LAST:event_jFormattedTextFieldCalibBgGeneXActionPerformed

    private void jFormattedTextFieldCalibBgGeneRefPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldCalibBgGeneRefPropertyChange
        // TODO add your handling code here:
        rna.calibBgGeneRef = ((Number)jFormattedTextFieldCalibBgGeneRef.getValue()).doubleValue();
    }//GEN-LAST:event_jFormattedTextFieldCalibBgGeneRefPropertyChange

    private void jFormattedTextFieldCalibBgGeneRefActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldCalibBgGeneRefActionPerformed
        // TODO add your handling code here:
        rna.calibBgGeneRef = ((Number)jFormattedTextFieldCalibBgGeneRef.getValue()).doubleValue();
    }//GEN-LAST:event_jFormattedTextFieldCalibBgGeneRefActionPerformed

    private void jComboBoxGeneXChActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxGeneXChActionPerformed
        // TODO add your handling code here:
        if (actionListener)
        rna.channels.add(2, jComboBoxGeneXCh.getSelectedItem().toString());
    }//GEN-LAST:event_jComboBoxGeneXChActionPerformed

    private void jFormattedTextFieldBgRoiSizePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldBgRoiSizePropertyChange
        // TODO add your handling code here:
        rna.roiBgSize = ((Number)jFormattedTextFieldBgRoiSize.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldBgRoiSizePropertyChange

    private void jFormattedTextFieldBgRoiSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldBgRoiSizeActionPerformed
        // TODO add your handling code here:
        rna.roiBgSize = ((Number)jFormattedTextFieldBgRoiSize.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldBgRoiSizeActionPerformed

    private void jComboBoxGeneRefChActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxGeneRefChActionPerformed
        // TODO add your handling code here:
        if (actionListener)
        rna.channels.add(1, jComboBoxGeneRefCh.getSelectedItem().toString());
    }//GEN-LAST:event_jComboBoxGeneRefChActionPerformed

    private void jComboBoxBgMethodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxBgMethodActionPerformed
        // TODO add your handling code here:
        rna.autoBackground = jComboBoxBgMethod.getSelectedItem().toString();
        switch (jComboBoxBgMethod.getSelectedIndex()) {
            case 0 :
            jFormattedTextFieldBgRoiSize.setEnabled(false);
            jFormattedTextFieldCalibBgGeneRef.setEnabled(false);
            jFormattedTextFieldCalibBgGeneX.setEnabled(false);
            break;
            case 1 :
            jFormattedTextFieldBgRoiSize.setEnabled(true);
            jFormattedTextFieldCalibBgGeneRef.setEnabled(false);
            jFormattedTextFieldCalibBgGeneX.setEnabled(false);
            break;
            case 2 :
            jFormattedTextFieldBgRoiSize.setEnabled(true);
            jFormattedTextFieldCalibBgGeneRef.setEnabled(true);
            jFormattedTextFieldCalibBgGeneX.setEnabled(true);
            break;
        }
    }//GEN-LAST:event_jComboBoxBgMethodActionPerformed

    private void jComboBoxBgMethodItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxBgMethodItemStateChanged
        // TODO add your handling code here:
        rna.autoBackground = jComboBoxBgMethod.getSelectedItem().toString();
        switch (jComboBoxBgMethod.getSelectedIndex()) {
            case 0 :
            jFormattedTextFieldBgRoiSize.setEnabled(false);
            jFormattedTextFieldCalibBgGeneRef.setEnabled(false);
            jFormattedTextFieldCalibBgGeneX.setEnabled(false);
            break;
            case 1 :
            jFormattedTextFieldBgRoiSize.setEnabled(true);
            jFormattedTextFieldCalibBgGeneRef.setEnabled(false);
            jFormattedTextFieldCalibBgGeneX.setEnabled(false);
            break;
            case 2 :
            jFormattedTextFieldBgRoiSize.setEnabled(true);
            jFormattedTextFieldCalibBgGeneRef.setEnabled(true);
            jFormattedTextFieldCalibBgGeneX.setEnabled(true);
            break;
        }
    }//GEN-LAST:event_jComboBoxBgMethodItemStateChanged

    private void jComboBoxDAPIChActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDAPIChActionPerformed
        // TODO add your handling code here:
        if (actionListener)
        rna.channels.add(0, jComboBoxDAPICh.getSelectedItem().toString());
    }//GEN-LAST:event_jComboBoxDAPIChActionPerformed

    private void jFormattedTextFieldGeneXSingleDotIntPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldGeneXSingleDotIntPropertyChange
        // TODO add your handling code here:
        rna.singleDotIntGeneX = ((Number)jFormattedTextFieldGeneXSingleDotInt.getValue()).doubleValue();
    }//GEN-LAST:event_jFormattedTextFieldGeneXSingleDotIntPropertyChange

    private void jFormattedTextFieldGeneXSingleDotIntActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldGeneXSingleDotIntActionPerformed
        // TODO add your handling code here:
        rna.singleDotIntGeneX = ((Number)jFormattedTextFieldGeneXSingleDotInt.getValue()).doubleValue();
    }//GEN-LAST:event_jFormattedTextFieldGeneXSingleDotIntActionPerformed

    private void jFormattedTextFieldGeneRefSingleDotIntPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldGeneRefSingleDotIntPropertyChange
        // TODO add your handling code here:
        rna.singleDotIntGeneRef = ((Number)jFormattedTextFieldGeneRefSingleDotInt.getValue()).doubleValue();
    }//GEN-LAST:event_jFormattedTextFieldGeneRefSingleDotIntPropertyChange

    private void jFormattedTextFieldGeneRefSingleDotIntActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldGeneRefSingleDotIntActionPerformed
        // TODO add your handling code here:
        rna.singleDotIntGeneRef = ((Number)jFormattedTextFieldGeneRefSingleDotInt.getValue()).doubleValue();
    }//GEN-LAST:event_jFormattedTextFieldGeneRefSingleDotIntActionPerformed

    private void jFormattedTextFieldSecToRemovePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldSecToRemovePropertyChange
        // TODO add your handling code here:
        rna.removeSlice = ((Number)jFormattedTextFieldSecToRemove.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldSecToRemovePropertyChange

    private void jFormattedTextFieldSecToRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldSecToRemoveActionPerformed
        // TODO add your handling code here:
        rna.removeSlice = ((Number)jFormattedTextFieldSecToRemove.getValue()).intValue();
    }//GEN-LAST:event_jFormattedTextFieldSecToRemoveActionPerformed

    private void jFormattedTextFieldNucDilPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFormattedTextFieldNucDilPropertyChange
        // TODO add your handling code here:
        rna.nucDil = ((Number)jFormattedTextFieldNucDil.getValue()).floatValue();
    }//GEN-LAST:event_jFormattedTextFieldNucDilPropertyChange

    private void jFormattedTextFieldNucDilActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextFieldNucDilActionPerformed
        // TODO add your handling code here:
        rna.nucDil = ((Number)jFormattedTextFieldNucDil.getValue()).floatValue();
    }//GEN-LAST:event_jFormattedTextFieldNucDilActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RNA_Scope_JDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                RNA_Scope_JDialog dialog = new RNA_Scope_JDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonBrowse;
    private javax.swing.JToggleButton jButtonCancel;
    private javax.swing.JButton jButtonConnect;
    private javax.swing.JToggleButton jButtonOk;
    private javax.swing.JCheckBox jCheckBoxNumberNucleus;
    private javax.swing.JComboBox jComboBoxBgMethod;
    private javax.swing.JComboBox jComboBoxDAPICh;
    private javax.swing.JComboBox<String> jComboBoxDatasets;
    private javax.swing.JComboBox jComboBoxGeneRefCh;
    private javax.swing.JComboBox jComboBoxGeneXCh;
    private javax.swing.JComboBox<String> jComboBoxProjects;
    private javax.swing.JComboBox jComboBoxSegMethod;
    private javax.swing.JComboBox jComboBoxThMethod;
    private javax.swing.JFormattedTextField jFormattedTextFieldBgRoiSize;
    private javax.swing.JFormattedTextField jFormattedTextFieldCalibBgGeneRef;
    private javax.swing.JFormattedTextField jFormattedTextFieldCalibBgGeneX;
    private javax.swing.JFormattedTextField jFormattedTextFieldCalibX;
    private javax.swing.JFormattedTextField jFormattedTextFieldCalibY;
    private javax.swing.JFormattedTextField jFormattedTextFieldCalibZ;
    private javax.swing.JFormattedTextField jFormattedTextFieldGeneRefSingleDotInt;
    private javax.swing.JFormattedTextField jFormattedTextFieldGeneXSingleDotInt;
    private javax.swing.JFormattedTextField jFormattedTextFieldMaxVol;
    private javax.swing.JFormattedTextField jFormattedTextFieldMinVol;
    private javax.swing.JFormattedTextField jFormattedTextFieldNucDil;
    private javax.swing.JFormattedTextField jFormattedTextFieldSecToRemove;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabelBg;
    private javax.swing.JLabel jLabelBgCalib;
    private javax.swing.JLabel jLabelBgMethod;
    private javax.swing.JLabel jLabelBgRoiSize;
    private javax.swing.JLabel jLabelCSpatialCalib;
    private javax.swing.JLabel jLabelCalibBgGeneRef;
    private javax.swing.JLabel jLabelCalibBgGeneX;
    private javax.swing.JLabel jLabelCalibX;
    private javax.swing.JLabel jLabelCalibY;
    private javax.swing.JLabel jLabelCalibZ;
    private javax.swing.JLabel jLabelChannels;
    private javax.swing.JLabel jLabelDAPICh;
    private javax.swing.JLabel jLabelDatasets;
    private javax.swing.JLabel jLabelGeneRefCh;
    private javax.swing.JLabel jLabelGeneRefSingleDotInt;
    private javax.swing.JLabel jLabelGeneXCh;
    private javax.swing.JLabel jLabelGeneXSingleDotInt;
    private javax.swing.JLabel jLabelImages;
    private javax.swing.JLabel jLabelImagesFolder;
    private javax.swing.JLabel jLabelMaxVol;
    private javax.swing.JLabel jLabelMinVol;
    private javax.swing.JLabel jLabelNucDil;
    private javax.swing.JLabel jLabelNucleus;
    private javax.swing.JLabel jLabelPassword;
    private javax.swing.JLabel jLabelPort;
    private javax.swing.JLabel jLabelProjects;
    private javax.swing.JLabel jLabelSecToRemove;
    private javax.swing.JLabel jLabelSegMethod;
    private javax.swing.JLabel jLabelSingleDotsCalib;
    private javax.swing.JLabel jLabelThMethod;
    private javax.swing.JLabel jLabelUser;
    private javax.swing.JPanel jPanelLocal;
    private javax.swing.JPanel jPanelOmero;
    private javax.swing.JPanel jPanelParameters;
    private javax.swing.JPasswordField jPasswordField;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPaneRNA_Scope;
    private javax.swing.JTextArea jTextAreaImages;
    private javax.swing.JTextField jTextFieldImagesFolder;
    private javax.swing.JTextField jTextFieldPort;
    private javax.swing.JTextField jTextFieldServerName;
    private javax.swing.JTextField jTextFieldUserID;
    // End of variables declaration//GEN-END:variables
}
