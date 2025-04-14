package com.syncfusion.services;


import com.google.api.services.directory.model.Member;
import com.syncfusion.database.models.TJSGroup;
import com.syncfusion.database.models.TJSGroupMember;
import com.syncfusion.database.repository.GroupMemberRepository;
import com.syncfusion.exceptions.TJSException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
public class GroupMemberService {

    Logger log = LoggerFactory.getLogger(GroupMemberService.class);
    @Autowired
    private GroupMemberRepository googleGroupMemberRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GoogleService googleService;


    public TJSGroupMember addMemberToTJSGroup(String societyId, String memberEmail, String firebaseUserId) {
        TJSGroup googleGroup = groupService.getGroupFromSocietyId(societyId);
        if (googleGroup == null) {
            throw new RuntimeException("Google Group not found for societyId: " + societyId);
        }
        if (StringUtils.isBlank(memberEmail) || StringUtils.isBlank(firebaseUserId)) {
            throw new RuntimeException("Invalid memberEmail or firebaseUserId");
        }

        TJSGroupMember googleGroupMember = new TJSGroupMember();
        googleGroupMember.setMemberEmail(memberEmail);
        googleGroupMember.setSocietyId(societyId);
        googleGroupMember.setFirebaseUserId(firebaseUserId);
        TJSGroupMember  groupMember = googleGroupMemberRepository.save(googleGroupMember);
        log.info("Added member :{},  to google group: {} ", memberEmail, googleGroup.getSocietyGroupEmail());
        return groupMember;
    }
    public TJSGroupMember addMemberToTJSGroupAndGoogleGroup(String societyId, String memberEmail, String firebaseUserId) throws IOException {
        TJSGroup group = groupService.getGroupFromSocietyId(societyId);
        if (group == null) {
            throw new TJSException("group not found for societyId: " + societyId, HttpStatus.NOT_FOUND);
        }
        String societyEmail = group.getSocietyGroupEmail();
        Member member = googleService.addMemberToGoogleGroup(societyEmail, memberEmail, "MEMBER");
        if (member == null) {
            throw new RuntimeException("Failed to add member to google group for societyId: " + societyId);
        }
        TJSGroupMember groupMember = addMemberToTJSGroup(societyId, memberEmail, firebaseUserId);
        return groupMember;
    }
    public void removeMemberFromTJSGroupAndGoogleGroup(String societyId, String memberEmail, String firebaseUserId) throws IOException {
        TJSGroup group = groupService.getGroupFromSocietyId(societyId);
        if(group == null) {
           throw new TJSException("group not found for societyId: " + societyId, HttpStatus.NOT_FOUND);
        }
        log.info("Removing member from google group for societyId: {}, groupEmail: {}, memberEmail: {}", societyId, group.getSocietyGroupEmail(), memberEmail);
        googleService.deleteMemberFromGoogleGroup(group.getSocietyGroupEmail(), memberEmail);
        log.info("Removed member from google group for societyId: {}, groupEmail: {}, memberEmail: {}", societyId, group.getSocietyGroupEmail(), memberEmail);

        log.info("Removing member from tjs group for societyId: {}, groupEmail: {}, memberEmail: {}", societyId, group.getSocietyGroupEmail(), memberEmail);
        removeMemberFromGroup(societyId, memberEmail, firebaseUserId);
        log.info("Removed member from tjs group for societyId: {}, groupEmail: {}, memberEmail: {}", societyId, group.getSocietyGroupEmail(), memberEmail);

    }

    public void removeMemberFromGroup(String societyId, String memberEmail, String firebaseUserId) {
        TJSGroup googleGroup = groupService.getGroupFromSocietyId(societyId);
        if (googleGroup == null) {
            throw new RuntimeException("Google Group not found for societyEmail: " + societyId);
        }
        if (StringUtils.isBlank(memberEmail)) {
            throw new RuntimeException("Invalid memberEmail");
        }
        log.info("Removing member :{},  from google group: {} ", memberEmail, googleGroup.getSocietyGroupEmail());
        TJSGroupMember googleGroupMember = findBySocietyIdAndMemberEmailAndFbUserId(societyId, memberEmail, firebaseUserId);
        if (googleGroupMember == null) {
            throw new RuntimeException("Member not found in google group");
        }
        googleGroupMemberRepository.delete(googleGroupMember);
        log.info("Removed member :{},  from google group: {} ", memberEmail, googleGroup.getSocietyGroupEmail());
    }

    public TJSGroupMember getTJSGroupFromMemberEmailAndSocietyId(String societyId, String memberEmail) {
        List<TJSGroupMember> members =  googleGroupMemberRepository.findBySocietyIdAndMemberEmail(societyId, memberEmail) ;
        if(members.size() > 1){
            log.error("Multiple members found for societyId: {}, memberEmail: {}", societyId, memberEmail);
        }
        return members.get(0);
    }
    public TJSGroupMember findBySocietyIdAndMemberEmailAndFbUserId(String societyId, String memberEmail, String fbUserId){
        return googleGroupMemberRepository.findBySocietyIdAndMemberEmailAndFirebaseUserId(societyId, memberEmail, fbUserId);
    }

}
