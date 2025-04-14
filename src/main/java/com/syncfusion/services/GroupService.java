package com.syncfusion.services;

import com.google.api.services.directory.model.Group;
import com.syncfusion.database.models.TJSGroup;
import com.syncfusion.database.repository.GroupRepository;
import com.syncfusion.dto.group.response.GoogleGroupResponse;
import com.syncfusion.exceptions.TJSException;
import com.syncfusion.utils.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class GroupService {

    Logger log = LoggerFactory.getLogger(GroupService.class);
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UtilMethods utilMethods;

    @Autowired
    private GoogleService googleService;

    public void deleteGroup(String societyId) {
        TJSGroup group = getGroupFromSocietyId(societyId);
        if (group == null) {
           throw new TJSException("Group not found for societyId: " + societyId, HttpStatus.NOT_FOUND);
        }
        groupRepository.delete(group);
        log.info("Deleted google group for societyId: {}, groupEmail: {}", societyId, group.getSocietyGroupEmail());
    }

    public TJSGroup addCustomGoogleGroup(String societyId, String societyEmail, String imposterEmail) {
        TJSGroup googleGroup = new TJSGroup();
        googleGroup.setSocietyId(societyId);
        googleGroup.setSocietyGroupEmail(societyEmail);
        googleGroup.setImposterEmail(imposterEmail);
        groupRepository.save(googleGroup);
        return googleGroup;
    }

    public TJSGroup createGoogleGroupAndTJSGroup(String societyId, String societyName, String societySeedEmailAddress) throws IOException {
        TJSGroup group = getGroupFromSocietyId(societyId);
        if (group != null) {
            throw new TJSException("Tejas Group already exists for societyId: " + societyId, HttpStatus.CONFLICT);
        }
        String proposedGroupEmail;
        if(StringUtils.isBlank(societySeedEmailAddress)){
             proposedGroupEmail = utilMethods.proposeEmailFromSocietyName(societyName.trim());
        }else{
            proposedGroupEmail = societySeedEmailAddress + "@" + utilMethods.getEmailDomain();
        }
        log.info("Creating.. new google group for societyId: {}, societyName: {}, proposedGroupEmail: {}", societyId, societyName, proposedGroupEmail);
        Group googleGroup;
        try {
            googleGroup = googleService.createNewGoogleGroup(proposedGroupEmail, societyName);
        } catch (Exception e) {
            log.info("Error while creating google group for societyId: {}, societyName: {}, groupEmail: {}", societyId, societyName, proposedGroupEmail);
            throw e;
        }
        log.info("Created new google group for societyId: {}, societyName: {}, groupEmail: {}", societyId, societyName, googleGroup.getEmail());

        log.info("updating google group settings for email :{}", googleGroup.getEmail());
        googleService.updateGoogleGroupSettings(googleGroup.getEmail());
        log.info("updated google group settings for email :{}", googleGroup.getEmail());

        log.info("adding imposter to group for societyId: {}, societyName: {}, groupEmail: {}", societyId, societyName, googleGroup.getEmail());
        addImposterToGroup(googleGroup.getEmail(), utilMethods.getImposterEmail());
        log.info("added imposter to group for societyId: {}, societyName: {}, groupEmail: {}", societyId, societyName, googleGroup.getEmail());
        TJSGroup tjsGroup = createTJSGroup(societyId, googleGroup.getEmail(), utilMethods.getImposterEmail());
        return tjsGroup;
    }
    public TJSGroup changeGroupSetting(String societyId, boolean active){
        TJSGroup group = getGroupFromSocietyId(societyId);
        if(group == null){
            throw new TJSException("Group not found for societyId: " + societyId, HttpStatus.NOT_FOUND);
        }
        group.setActive(active);
        group = groupRepository.save(group);
        return group;
    }

    public TJSGroup createTJSGroup(String societyId, String societyEmail, String imposterEmail){
        TJSGroup tjsGroup = new TJSGroup();
        tjsGroup.setSocietyId(societyId);
        tjsGroup.setSocietyGroupEmail(societyEmail);
        tjsGroup.setImposterEmail(utilMethods.getImposterEmail());
        log.info("saving tjs group for societyId: {} , groupEmail: {}", societyId, societyEmail);
        groupRepository.save(tjsGroup);
        log.info("saved tjs group for societyId: {}, , groupEmail: {}", societyId, societyEmail);
        return tjsGroup;
    }

    private void addImposterToGroup(String groupEmail, String imposterEmail) throws IOException {
        googleService.addMemberToGoogleGroup(groupEmail, imposterEmail, "OWNER");
    }


    public List<String> fetchAllGoogleGroups() {
        List<TJSGroup> allgroups = groupRepository.findAll();
        return allgroups.stream().map(TJSGroup::getSocietyGroupEmail).toList();
    }

    public boolean isMemberBelongsToSociety(String groupEmail, String memberEmail) {
        // add logic to check if member belongs to society
        return groupRepository.isMemberBelongsToSociety(groupEmail, memberEmail);
    }

    public TJSGroup getGroupFromGroupEmail(String groupEmail) {
        return groupRepository.findBySocietyGroupEmail(groupEmail);
    }

    public void createNewGoogleGroup(String societyId) {
        // add logic to create a new google group
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public void addMembersToGoogleGroup(String societyId, List<String> emails) {
        // add logic to add a member to a google group
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void removeMemberFromGoogleGroup(String societyId, String email) {
        // add logic to remove a member from a google group
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public TJSGroup getGroupFromSocietyId(String societyId) {
        return groupRepository.findBySocietyId(societyId);
    }

    private GoogleGroupResponse createGoogleGroupUsingGoogleAPI(String google_group_name) {
        // add logic to make http call to google group api
        return new GoogleGroupResponse("test_group@syncfusion.com", "test_group_id", "test_group_name");
    }

    public boolean doesImposterEmailExists(String imposterEmail) {
        return groupRepository.doesImposterEmailExists(imposterEmail);
    }
}

