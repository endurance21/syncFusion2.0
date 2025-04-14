package com.syncfusion.controllers.group;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.directory.model.Member;
import com.google.api.services.groupssettings.model.Groups;
import com.syncfusion.database.models.TJSGroup;
import com.syncfusion.database.models.TJSGroupMember;
import com.syncfusion.dto.group.request.*;
import com.syncfusion.dto.group.response.GroupResponse;
import com.syncfusion.dto.response.TJSResponse;
import com.syncfusion.exceptions.TJSException;
import com.syncfusion.services.GoogleService;
import com.syncfusion.services.GroupMemberService;
import com.syncfusion.services.GroupService;
import com.syncfusion.utils.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/group")
public class GroupController {

    Logger log = LoggerFactory.getLogger(GroupController.class);
    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupMemberService groupMemberService;

    @Autowired
    private UtilMethods utilMethods;

    @Autowired
    private GoogleService googleService;

    @PostMapping("")
    public ResponseEntity<GroupResponse<TJSGroup>> createGroup(@RequestBody GroupAddRequest requestBody) {
        try {
            TJSGroup googleGroup = groupService.createGoogleGroupAndTJSGroup(requestBody.getSocietyId(), requestBody.getSocietyName(), requestBody.getSocietySeedEmailAddress());
            GroupResponse groupResponse = new GroupResponse("Google Group created successfully", googleGroup);
            return utilMethods.getResponseEntity(groupResponse, HttpStatus.OK);
        } catch (Exception e) {
            if (e instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException googleJsonResponseException = ((GoogleJsonResponseException) e);
                return utilMethods.getResponseEntity(new GroupResponse<>(googleJsonResponseException.getMessage(), null), HttpStatus.valueOf(googleJsonResponseException.getStatusCode()));

            }
            if (e instanceof TJSException) {
                TJSException tjsException = (TJSException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), tjsException.getStatusCode());
            }
            log.error("Error creating google group", e);
            return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/custom")
    public ResponseEntity<GroupResponse<TJSGroup>> addNewGroup(@RequestBody CustomGroupAddRequest requestBody) {
        try {
            TJSGroup googleGroup = groupService.addCustomGoogleGroup(requestBody.getSocietyId(), requestBody.getGroupEmail(), requestBody.getImposterEmail());
            GroupResponse groupResponse = new GroupResponse("Google Group created successfully", googleGroup);
            return utilMethods.getResponseEntity(groupResponse, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error creating google group", e);
            return utilMethods.getResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("")
    public ResponseEntity<GroupResponse<TJSGroup>> deleteGroup(@RequestBody GroupDeleteRequest requestBody) {
        try {
            groupService.deleteGroup(requestBody.getSocietyId());
            GroupResponse groupResponse = new GroupResponse("Google Group deleted successfully", null);
            return utilMethods.getResponseEntity(groupResponse, HttpStatus.OK);
        } catch (Exception e) {
            if (e instanceof TJSException) {
                TJSException tjsException = (TJSException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), tjsException.getStatusCode());
            }
            log.error("Error creating google group", e);
            return utilMethods.getResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/email/{groupEmail}")
    public ResponseEntity<TJSResponse<TJSGroup,Object>> getGroupViaEmail(@PathVariable String groupEmail) {
        if (StringUtils.isBlank(groupEmail)) {
            return utilMethods.getResponseEntity(null, HttpStatus.BAD_REQUEST);
        }
        TJSGroup googleGroup = groupService.getGroupFromGroupEmail(groupEmail);
        TJSResponse response;
        if (googleGroup == null) {
            throw new TJSException("No group found", HttpStatus.NOT_FOUND);
        }
        response = TJSResponse.builder().data(googleGroup).message("sucess").status(HttpStatus.OK).build();
        return utilMethods.getResponseEntity(response, HttpStatus.OK);

    }

    @GetMapping("/societyId/{societyId}")
    public ResponseEntity<TJSResponse<TJSGroup,Object>> getGroupViaSocietyId(@PathVariable String societyId) {
        if (StringUtils.isBlank(societyId)) {
            return utilMethods.getResponseEntity(null, HttpStatus.BAD_REQUEST);
        }
        TJSGroup googleGroup = groupService.getGroupFromSocietyId(societyId);
        TJSResponse response;
        if (googleGroup == null) {
            throw new TJSException("No group found", HttpStatus.NOT_FOUND);
        }
        response = TJSResponse.builder().data(googleGroup).message("sucess").status(HttpStatus.OK).build();
        return utilMethods.getResponseEntity(response, HttpStatus.OK);
    }


    @PostMapping("/member")
    public ResponseEntity<GroupResponse<TJSGroupMember>> addMemberToGroup(@Validated @RequestBody GroupMemberAddRequest requestBody) {
        try {
            TJSGroupMember member = groupMemberService.addMemberToTJSGroupAndGoogleGroup(requestBody.getSocietyId(), requestBody.getMemberEmail(), requestBody.getFirebaseUserId());
            return utilMethods.getResponseEntity(new GroupResponse("Member added to google group", member), HttpStatus.OK);
        } catch (Exception e) {
            if (e instanceof TJSException) {
                TJSException tjsException = (TJSException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), tjsException.getStatusCode());
            }
            if (e instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException googleJsonResponseException = (GoogleJsonResponseException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), HttpStatus.valueOf(googleJsonResponseException.getStatusCode()));
            }
            log.error("Error adding member to google group", e);
            return utilMethods.getResponseEntity(new GroupResponse(e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/member/google")
    public ResponseEntity<GroupResponse<TJSGroupMember>> addMemberToGoogleGroup(@RequestBody GoogleGroupMemberAddRequest requestBody) {
        try {
             Member member = googleService.addMemberToGoogleGroup(requestBody.getSocietyEmail(), requestBody.getMemberEmail(),"MEMBER");
            return utilMethods.getResponseEntity(new GroupResponse("Member added to google group", member), HttpStatus.OK);
        } catch (Exception e) {
            if (e instanceof TJSException) {
                TJSException tjsException = (TJSException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), tjsException.getStatusCode());
            }
            if (e instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException googleJsonResponseException = (GoogleJsonResponseException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), HttpStatus.valueOf(googleJsonResponseException.getStatusCode()));
            }
            log.error("Error adding member to google group", e);
            return utilMethods.getResponseEntity(new GroupResponse(e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/member/custom")
    public ResponseEntity<GroupResponse<TJSGroupMember>> addCustomMemberToGroup(@Validated @RequestBody CustomGroupMemberAddRequest requestBody) {
        try {
            TJSGroupMember member = groupMemberService.addMemberToTJSGroup(requestBody.getSocietyId(), requestBody.getMemberEmail(), requestBody.getFirebaseUserId());
            return utilMethods.getResponseEntity(new GroupResponse("Member added to google group", member), HttpStatus.OK);
        } catch (Exception e) {
            if (e instanceof TJSException) {
                TJSException tjsException = (TJSException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), tjsException.getStatusCode());
            }
            if (e instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException googleJsonResponseException = (GoogleJsonResponseException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), HttpStatus.valueOf(googleJsonResponseException.getStatusCode()));
            }
            log.error("Error adding member to google group", e);
            return utilMethods.getResponseEntity(new GroupResponse(e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/member/delete")
    public ResponseEntity<GroupResponse<TJSGroupMember>> deleteMember(@Validated @RequestBody GroupMemberDeleteRequest requestBody) {
        try {
            groupMemberService.removeMemberFromTJSGroupAndGoogleGroup(requestBody.getSocietyId(), requestBody.getMemberEmail(), requestBody.getFirebaseUserId());
            return utilMethods.getResponseEntity(new GroupResponse("Member Removed  from  google group and tejas group", null), HttpStatus.OK);
        } catch (Exception e) {
            if (e instanceof TJSException) {
                TJSException tjsException = (TJSException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), tjsException.getStatusCode());
            }
            if (e instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException googleJsonResponseException = (GoogleJsonResponseException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), HttpStatus.valueOf(googleJsonResponseException.getStatusCode()));
            }
            log.error("Error adding member to google group", e);
            return utilMethods.getResponseEntity(new GroupResponse(e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/google/settings/{groupEmail}")
    public ResponseEntity<GroupResponse<Groups>> updateGoogleGroupSettings(@PathVariable String groupEmail) {
        if (StringUtils.isBlank(groupEmail)) {
            return utilMethods.getResponseEntity(new GroupResponse<>("Group email is blank", null), HttpStatus.BAD_REQUEST);
        }
        try {
            Groups groupSetting = googleService.updateGoogleGroupSettings(groupEmail);
            return utilMethods.getResponseEntity(new GroupResponse<>("Google group settings updated", groupSetting), HttpStatus.OK);
        } catch (Exception e) {
            if (e instanceof TJSException) {
                TJSException tjsException = (TJSException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), tjsException.getStatusCode());
            }
            if (e instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException googleJsonResponseException = (GoogleJsonResponseException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), HttpStatus.valueOf(googleJsonResponseException.getStatusCode()));
            }
            return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/google/settings/{groupEmail}")
    public ResponseEntity<GroupResponse<Groups>> getGoogleGroupSettings(@PathVariable String groupEmail) {
        if (StringUtils.isBlank(groupEmail)) {
            return utilMethods.getResponseEntity(new GroupResponse<>("Group email is blank", null), HttpStatus.BAD_REQUEST);
        }
        try {
            Groups groupSettings = googleService.getGoogleGroupSettings(groupEmail);
            return utilMethods.getResponseEntity(new GroupResponse<>("Sucess", groupSettings), HttpStatus.OK);
        } catch (Exception e) {
            if (e instanceof TJSException) {
                TJSException tjsException = (TJSException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), tjsException.getStatusCode());
            }
            if (e instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException googleJsonResponseException = (GoogleJsonResponseException) e;
                return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), HttpStatus.valueOf(googleJsonResponseException.getStatusCode()));
            }
            return utilMethods.getResponseEntity(new GroupResponse<>(e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PatchMapping("/settings")
    public ResponseEntity<TJSResponse<Object, Object>> changeGroupSetting(@Validated @RequestBody UpdateGroupSettingRequest request) throws Exception {
        try {
            TJSGroup group = groupService.changeGroupSetting(request.getSocietyId(), Boolean.parseBoolean(request.getActive()));
            TJSResponse response = TJSResponse.builder().message("Group setting updated").data(group).status(HttpStatus.OK).build();
            return utilMethods.getResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            if (!(e instanceof TJSException)) {
                e = new TJSException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating group setting", e);
            }
            throw e;
        }
    }


}


