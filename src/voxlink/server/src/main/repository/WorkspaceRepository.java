package voxlink.server.src.main.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import voxlink.server.src.main.database.DBConnection;
import voxlink.server.src.main.model.Workspace;

public class WorkspaceRepository {

    public boolean createWorkspace(Workspace workspace) {
        String query = "INSERT INTO workspaces (name, description, owner_id, invite_code) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, workspace.getName());
            pstmt.setString(2, workspace.getDescription());
            pstmt.setInt(3, workspace.getOwnerId());
            pstmt.setString(4, workspace.getInviteCode());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        workspace.setWorkspaceId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error creating workspace: " + e.getMessage());
        }
        return false;
    }

    public Workspace getWorkspaceById(int workspaceId) {
        String query = "SELECT * FROM workspaces WHERE workspace_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, workspaceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToWorkspace(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting workspace by ID: " + e.getMessage());
        }
        return null;
    }

    public Workspace getWorkspaceByInviteCode(String inviteCode) {
        String query = "SELECT * FROM workspaces WHERE invite_code = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, inviteCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToWorkspace(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting workspace by invite code: " + e.getMessage());
        }
        return null;
    }

    public List<Workspace> getWorkspacesByOwnerId(int ownerId) {
        List<Workspace> workspaces = new ArrayList<>();
        String query = "SELECT * FROM workspaces WHERE owner_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    workspaces.add(mapResultSetToWorkspace(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting workspaces by owner ID: " + e.getMessage());
        }
        return workspaces;
    }

    public boolean deleteWorkspace(int workspaceId) {
        String query = "DELETE FROM workspaces WHERE workspace_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, workspaceId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting workspace: " + e.getMessage());
        }
        return false;
    }

    private Workspace mapResultSetToWorkspace(ResultSet rs) throws SQLException {
        return new Workspace(
            rs.getInt("workspace_id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("owner_id"),
            rs.getString("invite_code"),
            rs.getTimestamp("created_at")
        );
    }
}
