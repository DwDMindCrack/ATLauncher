/**
 * Copyright 2013 by ATLauncher and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
 * Unported License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/3.0/.
 */
package com.atlauncher.workers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.atlauncher.App;
import com.atlauncher.data.Action;
import com.atlauncher.data.DecompType;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.Download;
import com.atlauncher.data.Downloadable;
import com.atlauncher.data.Instance;
import com.atlauncher.data.LogMessageType;
import com.atlauncher.data.MinecraftVersion;
import com.atlauncher.data.Mod;
import com.atlauncher.data.Pack;
import com.atlauncher.data.Type;
import com.atlauncher.gui.ModsChooser;
import com.atlauncher.utils.Utils;
import org.apache.commons.codec.binary.Base64;

public class InstanceInstaller extends SwingWorker<Boolean, Void> {

    private String instanceName;
    private Pack pack;
    private String version;
    private boolean isReinstall;
    private boolean isServer;
    private MinecraftVersion minecraftVersion;
    private String jarOrder;
    private boolean instanceIsCorrupt = false; // If the instance should be set as corrupt
    private boolean savedReis = false; // If Reis Minimap stuff was found and saved
    private boolean savedZans = false; // If Zans Minimap stuff was found and saved
    private boolean savedNEICfg = false; // If NEI Config was found and saved
    private boolean savedOptionsTxt = false; // If options.txt was found and saved
    private boolean savedServersDat = false; // If servers.dat was found and saved
    private boolean savedPortalGunSounds = false; // If Portal Gun Sounds was found and saved
    private boolean extractedTexturePack = false; // If there is an extracted texturepack
    private boolean extractedResourcePack = false; // If there is an extracted resourcepack
    private int permgen = 0;
    private int memory = 0;
    private String librariesNeeded = null;
    private String nativesNeeded = null;
    private String extraArguments = null;
    private String minecraftArguments = null;
    private String mainClass = null;
    private int percent = 0; // Percent done installing
    private ArrayList<Mod> allMods;
    private ArrayList<Mod> selectedMods;
    private int totalDownloads = 0; // Total number of downloads to download
    private int doneDownloads = 0; // Total number of downloads downloaded
    private int totalBytes = 0; // Total number of bytes to download
    private int downloadedBytes = 0; // Total number of bytes downloaded
    private Instance instance = null;
    private ArrayList<DisableableMod> modsInstalled;
    private ArrayList<File> serverLibraries;
    private ArrayList<Action> actions;

    public InstanceInstaller(String instanceName, Pack pack, String version,
            MinecraftVersion minecraftVersion, boolean isReinstall, boolean isServer) {
        this.instanceName = instanceName;
        this.pack = pack;
        this.version = version;
        this.minecraftVersion = minecraftVersion;
        this.isReinstall = isReinstall;
        this.isServer = isServer;
        if (isServer) {
            serverLibraries = new ArrayList<File>();
        }
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public boolean isServer() {
        return this.isServer;
    }

    public boolean isLegacy() {
        return this.minecraftVersion.isLegacy();
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    public ArrayList<DisableableMod> getModsInstalled() {
        return this.modsInstalled;
    }

    public String getInstanceSafeName() {
        return this.instanceName.replaceAll("[^A-Za-z0-9]", "");
    }

    public File getRootDirectory() {
        if (isServer) {
            return new File(App.settings.getServersDir(), pack.getSafeName() + "_"
                    + version.replaceAll("[^A-Za-z0-9]", ""));
        }
        return new File(App.settings.getInstancesDir(), getInstanceSafeName());
    }

    public File getTempDirectory() {
        return new File(App.settings.getTempDir(), pack.getSafeName() + "_"
                + version.replaceAll("[^A-Za-z0-9]", ""));
    }

    public File getTempJarDirectory() {
        return new File(App.settings.getTempDir(), pack.getSafeName() + "_"
                + version.replaceAll("[^A-Za-z0-9]", "") + "_JarTemp");
    }

    public File getTempActionsDirectory() {
        return new File(App.settings.getTempDir(), pack.getSafeName() + "_"
                + version.replaceAll("[^A-Za-z0-9]", "") + "_ActionsTemp");
    }

    public File getTempTexturePackDirectory() {
        return new File(App.settings.getTempDir(), pack.getSafeName() + "_"
                + version.replaceAll("[^A-Za-z0-9]", "") + "_TexturePackTemp");
    }

    public File getTempResourcePackDirectory() {
        return new File(App.settings.getTempDir(), pack.getSafeName() + "_"
                + version.replaceAll("[^A-Za-z0-9]", "") + "_ResourcePackTemp");
    }

    public File getLibrariesDirectory() {
        return new File(getRootDirectory(), "libraries");
    }

    public File getTexturePacksDirectory() {
        return new File(getRootDirectory(), "texturepacks");
    }

    public File getShaderPacksDirectory() {
        return new File(getRootDirectory(), "shaderpacks");
    }

    public File getResourcePacksDirectory() {
        return new File(getRootDirectory(), "resourcepacks");
    }

    public File getConfigDirectory() {
        return new File(getRootDirectory(), "config");
    }

    public File getModsDirectory() {
        return new File(getRootDirectory(), "mods");
    }

    public File getDependencyDirectory() {
        return new File(getModsDirectory(), this.minecraftVersion.getVersion());
    }

    public File getPluginsDirectory() {
        return new File(getRootDirectory(), "plugins");
    }

    public File getCoreModsDirectory() {
        return new File(getRootDirectory(), "coremods");
    }

    public File getJarModsDirectory() {
        return new File(getRootDirectory(), "jarmods");
    }

    public File getDisabledModsDirectory() {
        return new File(getRootDirectory(), "disabledmods");
    }

    public File getBinDirectory() {
        return new File(getRootDirectory(), "bin");
    }

    public File getNativesDirectory() {
        return new File(getBinDirectory(), "natives");
    }

    public boolean hasActions() {
        if (this.actions == null) {
            return false;
        }
        return this.actions.size() != 0;
    }

    public File getMinecraftJar() {
        if (isServer) {
            return new File(getRootDirectory(), "minecraft_server." + minecraftVersion + ".jar");
        }
        return new File(getBinDirectory(), "minecraft.jar");
    }

    public String getJarOrder() {
        return this.jarOrder;
    }

    public void setTexturePackExtracted() {
        this.extractedTexturePack = true;
    }

    public void setResourcePackExtracted() {
        this.extractedResourcePack = true;
    }

    public void addToJarOrder(String file) {
        if (jarOrder == null) {
            jarOrder = file;
        } else {
            if (!isLegacy()) {
                jarOrder = jarOrder + "," + file;
            } else {
                jarOrder = file + "," + jarOrder;
            }
        }
    }

    public boolean wasModInstalled(String mod) {
        if (instance != null) {
            return instance.wasModInstalled(mod);
        }
        return false;
    }

    public boolean isReinstall() {
        return this.isReinstall;
    }

    public boolean isModByName(String name) {
        for (Mod mod : allMods) {
            if (mod.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public Mod getModByName(String name) {
        for (Mod mod : allMods) {
            if (mod.getName().equalsIgnoreCase(name)) {
                return mod;
            }
        }
        return null;
    }

    public ArrayList<Mod> getLinkedMods(Mod mod) {
        ArrayList<Mod> linkedMods = new ArrayList<Mod>();
        for (Mod modd : allMods) {
            if (modd.getLinked().equalsIgnoreCase(mod.getName())) {
                linkedMods.add(modd);
            }
        }
        return linkedMods;
    }

    public ArrayList<Mod> getGroupedMods(Mod mod) {
        ArrayList<Mod> groupedMods = new ArrayList<Mod>();
        for (Mod modd : allMods) {
            if (modd.getGroup().equalsIgnoreCase(mod.getGroup())) {
                if (modd != mod) {
                    groupedMods.add(modd);
                }
            }
        }
        return groupedMods;
    }

    public ArrayList<Mod> getModsDependancies(Mod mod) {
        ArrayList<Mod> dependsMods = new ArrayList<Mod>();
        for (String name : mod.getDependancies()) {
            inner:
            {
                for (Mod modd : allMods) {
                    if (modd.getName().equalsIgnoreCase(name)) {
                        dependsMods.add(modd);
                        break inner;
                    }
                }
            }
        }
        return dependsMods;
    }

    public ArrayList<Mod> dependedMods(Mod mod) {
        ArrayList<Mod> dependedMods = new ArrayList<Mod>();
        for (Mod modd : allMods) {
            if (!modd.hasDepends()) {
                continue;
            }
            if (modd.isADependancy(mod)) {
                dependedMods.add(modd);
            }
        }
        return dependedMods;
    }

    public boolean hasADependancy(Mod mod) {
        for (Mod modd : allMods) {
            if (!modd.hasDepends()) {
                continue;
            }
            if (modd.isADependancy(mod)) {
                return true;
            }
        }
        return false;
    }

    private void makeDirectories() {
        if (isReinstall || isServer) {
            // We're reinstalling or installing a server so delete these folders
            Utils.delete(getBinDirectory());
            Utils.delete(getConfigDirectory());
            Utils.delete(getModsDirectory());
            Utils.delete(getCoreModsDirectory());
            if (isReinstall) {
                Utils.delete(getJarModsDirectory()); // Only delete if it's not a server
                Utils.delete(new File(getTexturePacksDirectory(), "TexturePack.zip"));
                Utils.delete(new File(getResourcePacksDirectory(), "ResourcePack.zip"));
            } else {
                Utils.delete(getLibrariesDirectory()); // Only delete if it's a server
            }
        }
        File[] directories;
        if (isServer) {
            directories = new File[]{getRootDirectory(), getModsDirectory(), getTempDirectory(),
                getLibrariesDirectory()};
        } else {
            directories = new File[]{getRootDirectory(), getModsDirectory(),
                getDisabledModsDirectory(), getTempDirectory(), getJarModsDirectory(),
                getBinDirectory(), getNativesDirectory()};
        }
        for (File directory : directories) {
            directory.mkdir();
        }
        if (minecraftVersion.usesCoreMods()) {
            getCoreModsDirectory().mkdir();
        }
    }

    private ArrayList<Downloadable> getDownloadableMods() {
        ArrayList<Downloadable> mods = new ArrayList<Downloadable>();

        String files = "";

        for (Mod mod : this.selectedMods) {
            if (mod.isServerDownload()) {
                files = files + mod.getURL() + "|||";
            }
        }

        HashMap<String, Integer> fileSizes = null;

        if (!files.isEmpty()) {
            try {
                String base64Files = Base64.encodeBase64String(files.getBytes("CP1252"));

                fileSizes = new HashMap<>();
                String returnValue = null;
                do {
                    try {
                        returnValue = Utils.sendPostData(App.settings.getFileURL("getfilesizes.php"),
                                base64Files, "files");
                    }
                    catch (IOException e) {
                        App.settings.logStackTrace(e);
                    }
                    if (returnValue == null) {
                        if (!App.settings.getNextServer()) {
                            App.settings
                                    .log("Couldn't get filesizes of files from all DwD servers. Continuing regardless!",
                                    LogMessageType.warning, false);
                        }
                    }
                } while (returnValue == null);
                if (returnValue != null) {
                    try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        InputSource is = new InputSource(new StringReader(returnValue));
                        Document document = builder.parse(is);
                        document.getDocumentElement().normalize();
                        NodeList nodeList = document.getElementsByTagName("file");
                        for (int i = 0; i < nodeList.getLength(); i++) {
                            Node node = nodeList.item(i);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                Element element = (Element) node;
                                String url = element.getAttribute("url");
                                int size = Integer.parseInt(element.getAttribute("size"));
                                fileSizes.put(url, size);
                            }
                        }
                    } catch (SAXException e) {
                        App.settings.logStackTrace(e);
                    } catch (ParserConfigurationException e) {
                        App.settings.logStackTrace(e);
                    } catch (IOException e) {
                        App.settings.logStackTrace(e);
                    }
                }
            } catch (Exception e) {
                App.settings.logStackTrace(e);
            }
        }

        for (Mod mod : this.selectedMods) {
            if (mod.isServerDownload()) {
                Downloadable downloadable;
                int size = -1;
                if (fileSizes.containsKey(mod.getURL())) {
                    size = fileSizes.get(mod.getURL());
                }
                if (mod.hasMD5()) {
                    downloadable = new Downloadable(mod.getURL(), new File(
                            App.settings.getDownloadsDir(), mod.getFile()), mod.getMD5(), size,
                            this, true);
                } else {
                    downloadable = new Downloadable(mod.getURL(), new File(
                            App.settings.getDownloadsDir(), mod.getFile()), null, size, this, true);
                }
                mods.add(downloadable);
            }
        }

        return mods;
    }

    private void loadActions() {
        this.actions = new ArrayList<Action>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(pack.getXML(version, false)));
            Document document = builder.parse(is);
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("action");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String mod = element.getAttribute("mod");
                    String action = element.getAttribute("action");
                    Type type = null;
                    if (element.hasAttribute("type")) {
                        type = Type.valueOf(element.getAttribute("type"));
                    }
                    String after = element.getAttribute("after");
                    String saveAs = element.getAttribute("saveas");
                    Boolean client = (element.getAttribute("client").equalsIgnoreCase("yes") ? true
                            : false);
                    Boolean server = (element.getAttribute("server").equalsIgnoreCase("yes") ? true
                            : false);
                    Action thing = null;
                    if (element.hasAttribute("type")) {
                        thing = new Action(action, type, after, saveAs, client, server);
                    } else {
                        thing = new Action(action, after, saveAs, client, server);
                    }
                    for (String modd : mod.split(",")) {
                        if (isModByName(modd)) {
                            thing.addMod(getModByName(modd));
                        }
                    }
                    actions.add(thing);
                }
            }
        } catch (SAXException e) {
            App.settings.logStackTrace(e);
        } catch (ParserConfigurationException e) {
            App.settings.logStackTrace(e);
        } catch (IOException e) {
            App.settings.logStackTrace(e);
        }
    }

    private void doActions() {
        for (Action action : this.actions) {
            action.execute(this);
        }
    }

    private boolean hasOptionalMods() {
        for (Mod mod : allMods) {
            if (mod.isOptional()) {
                return true;
            }
        }
        return false;
    }

    private void installMods(ArrayList<Mod> mods) {
        for (Mod mod : mods) {
            if (!isCancelled()) {
                fireTask(App.settings.getLocalizedString("common.installing") + " " + mod.getName());
                addPercent(mods.size() / 40);
                mod.install(this);
            }
        }
    }

    public boolean hasRecommendedMods() {
        for (Mod mod : allMods) {
            if (!mod.isRecommeneded()) {
                return true; // One of the mods is marked as not recommended, so return true
            }
        }
        return false; // No non recommended mods found
    }

    public boolean isOnlyRecommendedInGroup(Mod mod) {
        for (Mod modd : allMods) {
            if (modd == mod) {
                continue;
            }
            if (modd.getGroup().equalsIgnoreCase(mod.getGroup())) {
                if (modd.isRecommeneded()) {
                    return false; // Another mod is recommended. Don't check anything
                }
            }
        }
        return true; // No other recommended mods found in the group
    }

    private void downloadResources() {
        fireTask(App.settings.getLocalizedString("instance.downloadingresources"));
        fireSubProgressUnknown();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        ArrayList<Downloadable> downloads = getResources();
        totalBytes = 0;
        downloadedBytes = 0;

        for (Downloadable download : downloads) {
            if (download.needToDownload()) {
                totalBytes += download.getFilesize();
            }
        }

        fireSubProgress(0); // Show the subprogress bar
        for (final Downloadable download : downloads) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (download.needToDownload()) {
                        fireTask(App.settings.getLocalizedString("common.downloading") + " "
                                + download.getFile().getName());
                        download.download(true);
                    }
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        fireSubProgress(-1); // Hide the subprogress bar
    }

    private void downloadLibraries() {
        fireTask(App.settings.getLocalizedString("instance.downloadinglibraries"));
        fireSubProgressUnknown();
        ExecutorService executor;
        ArrayList<Downloadable> downloads = getLibraries();
        totalBytes = 0;
        downloadedBytes = 0;

        executor = Executors.newFixedThreadPool(8);

        for (final Downloadable download : downloads) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (download.needToDownload()) {
                        totalBytes += download.getFilesize();
                    }
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        fireSubProgress(0); // Show the subprogress bar

        executor = Executors.newFixedThreadPool(8);

        for (final Downloadable download : downloads) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (download.needToDownload()) {
                        fireTask(App.settings.getLocalizedString("common.downloading") + " "
                                + download.getFile().getName());
                        download.download(true);
                    }
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        fireSubProgress(-1); // Hide the subprogress bar
    }

    private void downloadMods(ArrayList<Mod> mods) {
        fireSubProgressUnknown();
        ExecutorService executor;
        ArrayList<Downloadable> downloads = getDownloadableMods();
        totalBytes = 0;
        downloadedBytes = 0;

        executor = Executors.newFixedThreadPool(8);

        for (final Downloadable download : downloads) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (download.needToDownload()) {
                        totalBytes += download.getFilesize();
                    }
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        fireSubProgress(0); // Show the subprogress bar

        executor = Executors.newFixedThreadPool(8);

        for (final Downloadable download : downloads) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (download.needToDownload()) {
                        download.download(true);
                    }
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        fireSubProgress(-1); // Hide the subprogress bar

        for (Mod mod : mods) {
            if (!downloads.contains(mod) && !isCancelled()) {
                fireTask(App.settings.getLocalizedString("common.downloading") + " "
                        + (mod.isFilePattern() ? mod.getName() : mod.getFile()));
                mod.download(this);
                fireSubProgress(-1); // Hide the subprogress bar
            }
        }
    }

    private void organiseLibraries() {
        fireTask(App.settings.getLocalizedString("instance.organisinglibraries"));
        fireSubProgressUnknown();
        if (!isServer) {
            String[] libraries = librariesNeeded.split(",");
            for (String libraryFile : libraries) {
                Utils.copyFile(new File(App.settings.getLibrariesDir(), libraryFile),
                        getBinDirectory());
            }
            String[] natives = nativesNeeded.split(",");
            for (String nativeFile : natives) {
                Utils.unzip(new File(App.settings.getLibrariesDir(), nativeFile),
                        getNativesDirectory());
            }
            Utils.delete(new File(getNativesDirectory(), "META-INF"));
        }
        if (isServer) {
            Utils.copyFile(new File(App.settings.getJarsDir(), "minecraft_server."
                    + this.minecraftVersion + ".jar"), getRootDirectory());
        } else {
            Utils.copyFile(new File(App.settings.getJarsDir(), this.minecraftVersion + ".jar"),
                    new File(getBinDirectory(), "minecraft.jar"), true);
        }
        fireSubProgress(-1); // Hide the subprogress bar
    }

    private ArrayList<Downloadable> getResources() {
        ArrayList<Downloadable> downloads = new ArrayList<Downloadable>(); // All the
        // files

        // Read in the resources needed

        if (!isServer) {
            try {
                boolean isTruncated;
                String marker = null;
                String add;
                do {
                    if (marker == null) {
                        add = "";
                    } else {
                        add = "?marker=" + marker;
                    }
                    URL resourceUrl = new URL("http://resources.download.minecraft.net/" + add);
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(resourceUrl.openStream());
                    isTruncated = Boolean.parseBoolean(doc.getElementsByTagName("IsTruncated")
                            .item(0).getTextContent());
                    NodeList nodeLst = doc.getElementsByTagName("Contents");
                    for (int i = 0; i < nodeLst.getLength(); i++) {
                        Node node = nodeLst.item(i);

                        if (node.getNodeType() == 1) {
                            Element element = (Element) node;
                            String key = element.getElementsByTagName("Key").item(0)
                                    .getChildNodes().item(0).getNodeValue();
                            String etag = element.getElementsByTagName("ETag") != null ? element
                                    .getElementsByTagName("ETag").item(0).getChildNodes().item(0)
                                    .getNodeValue() : "-";
                            etag = getEtag(etag);
                            marker = key;
                            int size = Integer.parseInt(element.getElementsByTagName("Size")
                                    .item(0).getChildNodes().item(0).getNodeValue());

                            if (size > 0) {
                                File file;
                                String filename;
                                if (key.contains("/")) {
                                    filename = key
                                            .substring(key.lastIndexOf('/') + 1, key.length());
                                    File directory = new File(App.settings.getResourcesDir(),
                                            key.substring(0, key.lastIndexOf('/')));
                                    file = new File(directory, filename);
                                } else {
                                    file = new File(App.settings.getResourcesDir(), key);
                                    filename = file.getName();
                                }
                                downloads.add(new Downloadable(
                                        "http://resources.download.minecraft.net/" + key, file,
                                        etag, size, this, false));
                            }
                        }
                    }
                } while (isTruncated);
            } catch (Exception e) {
                App.settings.logStackTrace(e);
            }
        }

        // Now lets see if we have custom mainclass and extraarguments

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(pack.getXML(version, false)));
            Document document = builder.parse(is);
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("mainclass");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    if (element.hasAttribute("depends")) {
                        boolean found = false;
                        for (Mod mod : selectedMods) {
                            if (element.getAttribute("depends").equalsIgnoreCase(mod.getName())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            break;
                        }
                    } else if (element.hasAttribute("dependsgroup")) {
                        boolean found = false;
                        for (Mod mod : selectedMods) {
                            if (element.getAttribute("dependsgroup").equalsIgnoreCase(
                                    mod.getGroup())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            break;
                        }
                    }
                    NodeList nodeList1 = element.getChildNodes();
                    this.mainClass = nodeList1.item(0).getNodeValue();
                }
            }
        } catch (SAXException e) {
            App.settings.logStackTrace(e);
        } catch (ParserConfigurationException e) {
            App.settings.logStackTrace(e);
        } catch (IOException e) {
            App.settings.logStackTrace(e);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(pack.getXML(version, false)));
            Document document = builder.parse(is);
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("extraarguments");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    if (element.hasAttribute("depends")) {
                        boolean found = false;
                        for (Mod mod : selectedMods) {
                            if (element.getAttribute("depends").equalsIgnoreCase(mod.getName())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            break;
                        }
                    } else if (element.hasAttribute("dependsgroup")) {
                        boolean found = false;
                        for (Mod mod : selectedMods) {
                            if (element.getAttribute("dependsgroup").equalsIgnoreCase(
                                    mod.getGroup())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            break;
                        }
                    }
                    NodeList nodeList1 = element.getChildNodes();
                    this.extraArguments = nodeList1.item(0).getNodeValue();
                }
            }
        } catch (SAXException e) {
            App.settings.logStackTrace(e);
        } catch (ParserConfigurationException e) {
            App.settings.logStackTrace(e);
        } catch (IOException e) {
            App.settings.logStackTrace(e);
        }

        return downloads;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Downloadable> getLibraries() {
        ArrayList<Downloadable> libraries = new ArrayList<Downloadable>();

        // Now read in the library jars needed from the pack
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(pack.getXML(version, false)));
            Document document = builder.parse(is);
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("library");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String url = element.getAttribute("url");
                    String file = element.getAttribute("file");
                    Download download = Download.direct;
                    if (element.hasAttribute("download")) {
                        download = Download.valueOf(element.getAttribute("download"));
                    }
                    String md5 = "-";
                    if (element.hasAttribute("md5")) {
                        md5 = element.getAttribute("md5");
                    }
                    if (element.hasAttribute("depends")) {
                        boolean found = false;
                        for (Mod mod : selectedMods) {
                            if (element.getAttribute("depends").equalsIgnoreCase(mod.getName())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            continue;
                        }
                    }
                    if (librariesNeeded == null) {
                        this.librariesNeeded = file;
                    } else {
                        this.librariesNeeded += "," + file;
                    }
                    File downloadTo = null;
                    if (isServer) {
                        if (!element.hasAttribute("server")) {
                            continue;
                        }
                        serverLibraries.add(new File(new File(getLibrariesDirectory(), element
                                .getAttribute("server").substring(0,
                                element.getAttribute("server").lastIndexOf('/'))), element
                                .getAttribute("server").substring(
                                element.getAttribute("server").lastIndexOf('/'),
                                element.getAttribute("server").length())));
                    }
                    downloadTo = new File(App.settings.getLibrariesDir(), file);
                    if (download == Download.server) {
                        libraries.add(new Downloadable(App.settings.getFileURL(url), downloadTo,
                                md5, this, false));
                    } else {
                        libraries.add(new Downloadable(url, downloadTo, md5, this, false));
                    }
                }
            }
        } catch (SAXException e) {
            App.settings.logStackTrace(e);
        } catch (ParserConfigurationException e) {
            App.settings.logStackTrace(e);
        } catch (IOException e) {
            App.settings.logStackTrace(e);
        }

        // Now read in the library jars needed from Mojang
        if (!isServer) {
            JSONParser parser = new JSONParser();

            try {
                Downloadable versionJson = new Downloadable(
                        "http://s3.amazonaws.com/Minecraft.Download/versions/"
                        + this.minecraftVersion + "/" + this.minecraftVersion + ".json",
                        false);
                Object obj = parser.parse(versionJson.getContents());
                JSONObject jsonObject = (JSONObject) obj;
                if (this.mainClass == null) {
                    this.mainClass = (String) jsonObject.get("mainClass");
                }
                this.minecraftArguments = (String) jsonObject.get("minecraftArguments");
                JSONArray msg = (JSONArray) jsonObject.get("libraries");
                Iterator<JSONObject> iterator = msg.iterator();
                while (iterator.hasNext()) {
                    boolean shouldDownload = false;
                    JSONObject object = iterator.next();
                    String name = (String) object.get("name");
                    if (isLegacy()) {
                        if (!name.contains("jinput") && !name.contains("lwjgl")) {
                            continue;
                        }
                    }
                    String[] parts = name.split(":");
                    String dir = parts[0].replace(".", "/") + "/" + parts[1] + "/" + parts[2];
                    String filename = null;
                    if (object.containsKey("rules")) {
                        JSONArray ruless = (JSONArray) object.get("rules");
                        Iterator<JSONObject> itt = ruless.iterator();
                        while (itt.hasNext()) {
                            JSONObject rules = itt.next();
                            if (((String) rules.get("action")).equalsIgnoreCase("allow")) {
                                if (rules.containsKey("os")) {
                                    JSONObject rule = (JSONObject) rules.get("os");
                                    if (((String) rule.get("name")).equalsIgnoreCase(Utils
                                            .getOSName())) {
                                        if (rule.containsKey("version")) {
                                            Pattern pattern = Pattern.compile((String) rule
                                                    .get("version"));
                                            Matcher matcher = pattern.matcher(System
                                                    .getProperty("os.version"));
                                            if (matcher.matches()) {
                                                shouldDownload = true;
                                            }
                                        } else {
                                            shouldDownload = true;
                                        }
                                    }
                                } else {
                                    shouldDownload = true;
                                }
                            } else if (((String) rules.get("action")).equalsIgnoreCase("disallow")) {
                                if (rules.containsKey("os")) {
                                    JSONObject rule = (JSONObject) rules.get("os");
                                    if (((String) rule.get("name")).equalsIgnoreCase(Utils
                                            .getOSName())) {
                                        if (rule.containsKey("version")) {
                                            Pattern pattern = Pattern.compile((String) rule
                                                    .get("version"));
                                            Matcher matcher = pattern.matcher(System
                                                    .getProperty("os.version"));
                                            if (matcher.matches()) {
                                                shouldDownload = false;
                                            }
                                        } else {
                                            shouldDownload = false;
                                        }
                                    }
                                }
                            } else {
                                shouldDownload = true;
                            }
                        }
                    } else {
                        shouldDownload = true;
                    }

                    if (shouldDownload) {
                        if (object.containsKey("natives")) {
                            JSONObject nativesObject = (JSONObject) object.get("natives");
                            String nativesName;
                            if (Utils.isWindows()) {
                                nativesName = (String) nativesObject.get("windows");
                            } else if (Utils.isMac()) {
                                nativesName = (String) nativesObject.get("osx");
                            } else {
                                nativesName = (String) nativesObject.get("linux");
                            }
                            filename = parts[1] + "-" + parts[2] + "-" + nativesName + ".jar";
                            if (nativesNeeded == null) {
                                this.nativesNeeded = filename;
                            } else {
                                this.nativesNeeded += "," + filename;
                            }
                        } else {
                            filename = parts[1] + "-" + parts[2] + ".jar";
                            if (librariesNeeded == null) {
                                this.librariesNeeded = filename;
                            } else {
                                this.librariesNeeded += "," + filename;
                            }
                        }
                        String url = "https://libraries.minecraft.net/" + dir
                                + "/" + filename;
                        File file = new File(App.settings.getLibrariesDir(), filename);
                        libraries.add(new Downloadable(url, file, null, this, false));
                    }
                }

            } catch (ParseException e) {
                App.settings.logStackTrace(e);
            }
        }

        if (isServer) {
            libraries.add(new Downloadable(
                    "http://s3.amazonaws.com/Minecraft.Download/versions/" + this.minecraftVersion
                    + "/minecraft_server." + this.minecraftVersion + ".jar", new File(
                    App.settings.getJarsDir(), "minecraft_server." + this.minecraftVersion
                    + ".jar"), null, this, false));
        } else {
            libraries.add(new Downloadable("http://s3.amazonaws.com/Minecraft.Download/versions/"
                    + this.minecraftVersion + "/" + this.minecraftVersion + ".jar", new File(
                    App.settings.getJarsDir(), this.minecraftVersion + ".jar"), null, this, false));
        }
        return libraries;
    }

    public static String getEtag(String etag) {
        if (etag == null) {
            etag = "-";
        } else if ((etag.startsWith("\"")) && (etag.endsWith("\""))) {
            etag = etag.substring(1, etag.length() - 1);
        }
        return etag;
    }

    public void deleteMetaInf() {
        File inputFile = getMinecraftJar();
        File outputTmpFile = new File(App.settings.getTempDir(), pack.getSafeName()
                + "-minecraft.jar");
        try {
            JarInputStream input = new JarInputStream(new FileInputStream(inputFile));
            JarOutputStream output = new JarOutputStream(new FileOutputStream(outputTmpFile));
            JarEntry entry;

            while ((entry = input.getNextJarEntry()) != null) {
                if (entry.getName().contains("META-INF")) {
                    continue;
                }
                output.putNextEntry(entry);
                byte buffer[] = new byte[1024];
                int amo;
                while ((amo = input.read(buffer, 0, 1024)) != -1) {
                    output.write(buffer, 0, amo);
                }
                output.closeEntry();
            }

            input.close();
            output.close();

            inputFile.delete();
            outputTmpFile.renameTo(inputFile);
        } catch (IOException e) {
            App.settings.logStackTrace(e);
        }
    }

    public void configurePack() {
        // Download the configs zip file
        fireTask(App.settings.getLocalizedString("instance.downloadingconfigs"));
        File configs = new File(App.settings.getTempDir(), "Configs.zip");
        String path = "packs/" + pack.getSafeName() + "/versions/" + version + "/Configs.zip";
        Downloadable configsDownload = new Downloadable(path, configs, null, this, true);
        this.totalBytes = configsDownload.getFilesize();
        this.downloadedBytes = 0;
        configsDownload.download(true); // Download the file

        // Extract the configs zip file
        fireSubProgressUnknown();
        fireTask(App.settings.getLocalizedString("instance.extractingconfigs"));
        Utils.unzip(configs, getRootDirectory());
        Utils.delete(configs);

        // Copy over common configs if any
        if (App.settings.getCommonConfigsDir().listFiles().length != 0) {
            Utils.copyDirectory(App.settings.getCommonConfigsDir(), getRootDirectory());
        }
    }

    public String getServerJar() {
        Mod forge = null; // The Forge Mod
        Mod mcpc = null; // The MCPC Mod
        for (Mod mod : selectedMods) {
            if (mod.getType() == Type.forge) {
                forge = mod;
            } else if (mod.getType() == Type.mcpc) {
                mcpc = mod;
            }
        }
        if (mcpc != null) {
            return mcpc.getFile();
        } else if (forge != null) {
            return forge.getFile();
        } else {
            return "minecraft_server." + minecraftVersion + ".jar";
        }
    }

    public boolean hasJarMods() {
        for (Mod mod : selectedMods) {
            if (!mod.installOnServer() && isServer) {
                continue;
            }
            if (mod.getType() == Type.jar) {
                return true;
            } else if (mod.getType() == Type.decomp) {
                if (mod.getDecompType() == DecompType.jar) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasForge() {
        for (Mod mod : selectedMods) {
            if (mod.getType() == Type.forge) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Mod> getMods() {
        return this.allMods;
    }

    public boolean shouldCoruptInstance() {
        return this.instanceIsCorrupt;
    }

    public MinecraftVersion getMinecraftVersion() {
        return this.minecraftVersion;
    }

    public int getPermGen() {
        return this.permgen;
    }

    public int getMemory() {
        return this.memory;
    }

    public String getLibrariesNeeded() {
        return this.librariesNeeded;
    }

    public String getExtraArguments() {
        return this.extraArguments;
    }

    public String getMinecraftArguments() {
        return this.minecraftArguments;
    }

    public String getMainClass() {
        return this.mainClass;
    }

    public ArrayList<Mod> sortMods(ArrayList<Mod> original) {
        ArrayList<Mod> mods = new ArrayList<Mod>(original);

        for (Mod mod : original) {
            if (mod.isOptional()) {
                if (!mod.getLinked().isEmpty()) {
                    for (Mod mod1 : original) {
                        if (mod1.getName().equalsIgnoreCase(mod.getLinked())) {
                            mods.remove(mod);
                            int index = mods.indexOf(mod1) + 1;
                            mods.add(index, mod);
                        }
                    }

                }
            }
        }

        return mods;
    }

    protected Boolean doInBackground() throws Exception {
        if (this.isReinstall) {
            System.out.println(this.pack.getUpdateMessage(this.version));
        } else {
            System.out.println(this.pack.getInstallMessage(this.version));
        }
        this.allMods = sortMods(this.pack.getMods(this.version, isServer));
        loadActions(); // Load all the actions up for the pack
        this.permgen = this.pack.getPermGen(this.version);
        this.memory = this.pack.getMemory(this.version);
        if (this.minecraftVersion == null) {
            this.cancel(true);
            return false;
        }
        selectedMods = new ArrayList<Mod>();
        if (allMods.size() != 0 && hasOptionalMods()) {
            ModsChooser modsChooser = new ModsChooser(this);
            modsChooser.setVisible(true);
            if (modsChooser.wasClosed()) {
                this.cancel(true);
                return false;
            }
            selectedMods = modsChooser.getSelectedMods();
        }
        if (!hasOptionalMods()) {
            selectedMods = allMods;
        }
        modsInstalled = new ArrayList<DisableableMod>();
        for (Mod mod : selectedMods) {
            modsInstalled.add(new DisableableMod(mod.getName(), mod.getVersion(), mod.isOptional(),
                    mod.getFile(), mod.getType(), mod.getColour(), mod.getDescription(), false));
        }
        this.instanceIsCorrupt = true; // From this point on the instance is corrupt
        makeDirectories();
        addPercent(5);
        File reis = new File(getModsDirectory(), "rei_minimap");
        if (reis.exists() && reis.isDirectory()) {
            if (Utils.copyDirectory(reis, getTempDirectory(), true)) {
                savedReis = true;
            }
        }
        File zans = new File(getModsDirectory(), "VoxelMods");
        if (zans.exists() && zans.isDirectory()) {
            if (Utils.copyDirectory(zans, getTempDirectory(), true)) {
                savedZans = true;
            }
        }
        File neiCfg = new File(getConfigDirectory(), "NEI.cfg");
        if (neiCfg.exists() && neiCfg.isFile()) {
            if (Utils.copyFile(neiCfg, getTempDirectory())) {
                savedNEICfg = true;
            }
        }
        File optionsTXT = new File(getRootDirectory(), "options.txt");
        if (optionsTXT.exists() && optionsTXT.isFile()) {
            if (Utils.copyFile(optionsTXT, getTempDirectory())) {
                savedOptionsTxt = true;
            }
        }
        File serversDAT = new File(getRootDirectory(), "servers.dat");
        if (serversDAT.exists() && serversDAT.isFile()) {
            if (Utils.copyFile(serversDAT, getTempDirectory())) {
                savedServersDat = true;
            }
        }
        File portalGunSounds = new File(getModsDirectory(), "PortalGunSounds.pak");
        if (portalGunSounds.exists() && portalGunSounds.isFile()) {
            savedPortalGunSounds = true;
            Utils.copyFile(portalGunSounds, getTempDirectory());
        }
        downloadResources(); // Download Minecraft Resources
        if (isCancelled()) {
            return false;
        }
        downloadLibraries(); // Download Libraries
        if (isCancelled()) {
            return false;
        }
        organiseLibraries(); // Organise the libraries
        if (isCancelled()) {
            return false;
        }
        if (isServer) {
            for (File file : serverLibraries) {
                file.mkdirs();
                Utils.copyFile(new File(App.settings.getLibrariesDir(), file.getName()), file, true);
            }
        }
        addPercent(5);
        if (isServer && hasJarMods()) {
            fireTask(App.settings.getLocalizedString("server.extractingjar"));
            fireSubProgressUnknown();
            Utils.unzip(getMinecraftJar(), getTempJarDirectory());
        }
        if (!isServer && hasJarMods() && !hasForge()) {
            deleteMetaInf();
        }
        addPercent(5);
        if (selectedMods.size() != 0) {
            addPercent(40);
            fireTask(App.settings.getLocalizedString("instance.downloadingmods"));
            downloadMods(selectedMods);
            if (isCancelled()) {
                return false;
            }
            addPercent(40);
            installMods(selectedMods);
        } else {
            addPercent(80);
        }
        if (isCancelled()) {
            return false;
        }
        if (isServer && hasJarMods()) {
            fireTask(App.settings.getLocalizedString("server.zippingjar"));
            fireSubProgressUnknown();
            Utils.zip(getTempJarDirectory(), getMinecraftJar());
        }
        if (extractedTexturePack) {
            fireTask(App.settings.getLocalizedString("instance.zippingtexturepackfiles"));
            fireSubProgressUnknown();
            if (!getTexturePacksDirectory().exists()) {
                getTexturePacksDirectory().mkdir();
            }
            Utils.zip(getTempTexturePackDirectory(), new File(getTexturePacksDirectory(),
                    "TexturePack.zip"));
        }
        if (extractedResourcePack) {
            fireTask(App.settings.getLocalizedString("instance.zippingresourcepackfiles"));
            fireSubProgressUnknown();
            if (!getResourcePacksDirectory().exists()) {
                getResourcePacksDirectory().mkdir();
            }
            Utils.zip(getTempResourcePackDirectory(), new File(getResourcePacksDirectory(),
                    "ResourcePack.zip"));
        }
        if (isCancelled()) {
            return false;
        }
        if (hasActions()) {
            doActions();
        }
        if (isCancelled()) {
            return false;
        }
        configurePack();
        if (savedReis) {
            Utils.copyDirectory(new File(getTempDirectory(), "rei_minimap"), reis);
        }
        if (savedZans) {
            Utils.copyDirectory(new File(getTempDirectory(), "VoxelMods"), zans);
        }
        if (savedNEICfg) {
            Utils.copyFile(new File(getTempDirectory(), "NEI.cfg"), neiCfg, true);
        }
        if (savedOptionsTxt) {
            Utils.copyFile(new File(getTempDirectory(), "options.txt"), optionsTXT, true);
        }
        if (savedServersDat) {
            Utils.copyFile(new File(getTempDirectory(), "servers.dat"), serversDAT, true);
        }
        if (savedPortalGunSounds) {
            Utils.copyFile(new File(getTempDirectory(), "PortalGunSounds.pak"), portalGunSounds,
                    true);
        }
        if (isServer) {
            Utils.replaceText(new File(App.settings.getLibrariesDir(), "LaunchServer.bat"),
                    new File(getRootDirectory(), "LaunchServer.bat"), "%%SERVERJAR%%",
                    getServerJar());
            Utils.replaceText(new File(App.settings.getLibrariesDir(), "LaunchServer.sh"),
                    new File(getRootDirectory(), "LaunchServer.sh"), "%%SERVERJAR%%",
                    getServerJar());
        }
        return true;
    }

    public void resetDownloadedBytes(int bytes) {
        totalBytes = bytes;
        downloadedBytes = 0;
    }

    public void fireTask(String name) {
        firePropertyChange("doing", null, name);
    }

    private void fireProgress(int percent) {
        if (percent > 100) {
            percent = 100;
        }
        firePropertyChange("progress", null, percent);
    }

    private void fireSubProgress(int percent) {
        if (percent > 100) {
            percent = 100;
        }
        firePropertyChange("subprogress", null, percent);
    }

    private void fireSubProgress(int percent, String paint) {
        if (percent > 100) {
            percent = 100;
        }
        String[] info = new String[2];
        info[0] = "" + percent;
        info[1] = paint;
        firePropertyChange("subprogress", null, info);
    }

    public void fireSubProgressUnknown() {
        firePropertyChange("subprogressint", null, null);
    }

    private void addPercent(int percent) {
        this.percent = this.percent + percent;
        if (this.percent > 100) {
            this.percent = 100;
        }
        fireProgress(this.percent);
    }

    public void setSubPercent(int percent) {
        if (percent > 100) {
            percent = 100;
        }
        fireSubProgress(percent);
    }

    public void setDownloadDone() {
        this.doneDownloads++;
        float progress;
        if (this.totalDownloads > 0) {
            progress = ((float) this.doneDownloads / (float) this.totalDownloads) * 100;
        } else {
            progress = 0;
        }
        fireSubProgress((int) progress);
    }

    public void addDownloadedBytes(int bytes) {
        this.downloadedBytes += bytes;
        float progress;
        if (this.totalBytes > 0) {
            progress = ((float) this.downloadedBytes / (float) this.totalBytes) * 100;
        } else {
            progress = 0;
        }
        fireSubProgress((int) progress,
                String.format("%.2f", ((float) this.downloadedBytes / 1024 / 1024)) + " MB / "
                + String.format("%.2f", ((float) this.totalBytes / 1024 / 1024)) + " MB");
    }
}
