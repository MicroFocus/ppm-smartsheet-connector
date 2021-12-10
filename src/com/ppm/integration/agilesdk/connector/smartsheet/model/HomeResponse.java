package com.ppm.integration.agilesdk.connector.smartsheet.model;

import com.ppm.integration.agilesdk.connector.smartsheet.SmartsheetConstants;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class HomeResponse {
    public SmartsheetSheet[] sheets; // Sheets in the home root
    public Folder[] folders; // folders in Home (possibly with sub folders)
    public Workspace[] workspaces; // Workspaces in Home (possibly with folders in them)

    public class Folder {
        public String id;
        public String name;
        public String permalink;
        public Folder[] folders; // sub folders
        public SmartsheetSheet[] sheets; // Sheets in this folder
        public String parentPath;

        public String getFullName() {
            return parentPath == null ? name : (parentPath + name);
        }
    }

    public class Workspace {
        public String id;
        public String name;
        public String accessLevel;
        public String permalink;
        public Folder[] folders; // folders in workspace
        public SmartsheetSheet[] sheets; // Sheets in this workspace
        // Workspaces don't have sub-workspaces
    }


    public List<Folder> getAllFolders() {
        List<Folder> foldersList = new ArrayList<>();

        if (workspaces != null) {
            for (Workspace workspace : workspaces) {
                addFolders(workspace.folders, foldersList, "["+workspace.name+"]/");
            }
        }

        addFolders(folders, foldersList, "");

        return foldersList;
    }

    private void addFolders(Folder[] folders, List<Folder> foldersList, String parentPath) {
        if (folders == null) {
            return;
        }

        for (Folder folder : folders) {
            folder.parentPath = parentPath;
            foldersList.add(folder);

            addFolders(folder.folders, foldersList, parentPath + folder.name + "/");
        }
    }


    // Following methods are used to retrieve the sheets matching some potential folder/workspace filters.
    //////////////

    public List<SmartsheetSheet> getAllSheets() {
        List<SmartsheetSheet> sheetsList = new ArrayList<>();

        addSheets(sheets, sheetsList, SmartsheetConstants.HOME_PATH);

        if (workspaces != null) {
            for (Workspace workspace : workspaces) {
                addWorkspaceSheets(workspace, sheetsList, true);
            }
        }

        if (folders != null) {
            for (Folder folder: folders) {
                addFolderSheets(folder, sheetsList, SmartsheetConstants.HOME_PATH);
            }
        }

        return sheetsList;
    }

    public List<SmartsheetSheet> getWorkspaceSheets(String workspaceId) {

        List<SmartsheetSheet> sheetsList = new ArrayList<>();

        if (StringUtils.isBlank(workspaceId)) {
            return sheetsList;
        }

        // Find the right workspace
        if (workspaces != null) {
            for (Workspace workspace : workspaces) {
                if (workspaceId.equals(workspace.id)) {
                    addWorkspaceSheets(workspace, sheetsList, false);
                }
            }
        }

        return sheetsList;
    }

    public List<SmartsheetSheet> getFolderSheets(String folderId) {

        List<SmartsheetSheet> sheetsList = new ArrayList<>();

        if (StringUtils.isBlank(folderId)) {
            return sheetsList;
        }

        // Let's look for the right folder, first in Home
        Folder folder = findFolder(folders, folderId);

        if (folder == null) {
            // if not found, check in all workspaces.
            if (workspaces != null) {
                for (Workspace workspace : workspaces) {
                    folder = findFolder(workspace.folders, folderId);
                    if (folder != null) break;
                }
            }
        }

        addFolderSheets(folder, sheetsList, "");

        return sheetsList;
    }

    /**
     * Look for the folder in the passed folders and all their sub-folders.
     * folderId is not null safe.
     */
    private Folder findFolder(Folder[] folders, String folderId) {
        if (folders == null) {
            return null;
        }

        for (Folder folder : folders) {
            if (folderId.equals(folder.id)) {
                return folder;
            }

            Folder folderInSub = findFolder(folder.folders, folderId);
            if (folderInSub != null) {
                return folderInSub;
            }
        }

        return null;
    }

    private void addWorkspaceSheets(Workspace workspace, List<SmartsheetSheet> sheetsList, boolean includeWorkspaceInPath) {
        if (workspace == null) {
            return;
        }

        String workspacePath = includeWorkspaceInPath ? "["+workspace.name+"]/" : "";

        // Add any sheet in this workspace
        addSheets(workspace.sheets, sheetsList, workspacePath);

        // Add any sheet in this workspace folders
        if (workspace.folders != null) {
            for (Folder folder: workspace.folders) {
                addFolderSheets(folder, sheetsList, workspacePath);
            }
        }
    }

    private void addFolderSheets(Folder folder, List<SmartsheetSheet> sheetsList, String parentPath) {
        if (folder == null) {
            return;
        }

        // We first add any sheet from the current folder
        addSheets(folder.sheets, sheetsList, parentPath + folder.name + "/");

        // And then we add sheets from all children sub-folders;
        if (folder.folders != null) {
            for (Folder subFolder : folder.folders) {
                addFolderSheets(subFolder, sheetsList, parentPath + folder.name + "/" );
            }
        }
    }

    private void addSheets(SmartsheetSheet[] sheets, List<SmartsheetSheet> sheetsList, String path) {
        if (sheets == null) {
            return;
        }

        for (SmartsheetSheet sheet: sheets) {
            sheet.path = path;
            sheetsList.add(sheet);
        }
    }
}
