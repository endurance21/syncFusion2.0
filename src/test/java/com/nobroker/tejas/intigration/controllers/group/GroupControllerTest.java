package com.syncfusion.intigration.controllers.group;

import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.Member;
import com.syncfusion.database.models.TJSGroup;
import com.syncfusion.database.models.TJSGroupMember;
import com.syncfusion.dto.group.request.GroupAddRequest;
import com.syncfusion.dto.group.request.GroupDeleteRequest;
import com.syncfusion.dto.group.request.GroupMemberAddRequest;
import com.syncfusion.dto.group.request.GroupMemberDeleteRequest;
import com.syncfusion.intigration.AbstractIntegrationTest;
import com.syncfusion.services.GoogleService;
import com.syncfusion.services.GroupMemberService;
import com.syncfusion.services.GroupService;
import com.syncfusion.utils.UtilMethods;
import org.instancio.Instancio;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class GroupControllerTest extends AbstractIntegrationTest {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilMethods utilsMethods;

    @MockBean
    private GoogleService googleService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupMemberService groupMemberService;

    @Test
    public void canHandleNewGroupCreation() throws Exception {
        logger.info("Running test for new group creation");
        Group fakeGoogleGroup = Instancio.create(Group.class);
        fakeGoogleGroup.setEmail("some-email@nobroker.in");
        when(googleService.createNewGoogleGroup(any(), any())).thenReturn(fakeGoogleGroup);
        GroupAddRequest payload = Instancio.create(GroupAddRequest.class);
        mockMvc.perform(post("/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json().build().writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    public void canHandleGroupDeletion() throws Exception {
        logger.info("Running test for  group deletion");
        GroupDeleteRequest fakeGroupDeleteRequest = Instancio.create(GroupDeleteRequest.class);
        //first create in db
        groupService.createTJSGroup(fakeGroupDeleteRequest.getSocietyId(), "some-email@nobroker.in", utilsMethods.getImposterEmail());

        // check if we can delet it as well
        mockMvc.perform(delete("/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json().build().writeValueAsString(fakeGroupDeleteRequest)))
                .andExpect(status().isOk());

        // check if it is deleted succesfully
        TJSGroup group = groupService.getGroupFromSocietyId(fakeGroupDeleteRequest.getSocietyId());
        assertNull(group);

    }

    @Test
    public void canHandleNewGroupMemberAddition() throws Exception {
        logger.info("Running test for new group member addition");

        Member fakeGoogleGroupMember = Instancio.create(Member.class);
        when(googleService.addMemberToGoogleGroup(any(), any(), any())).thenReturn(fakeGoogleGroupMember);

        GroupMemberAddRequest fakeGroupMemberAddRequest = Instancio.create(GroupMemberAddRequest.class);
        //first a group create in db
        groupService.createTJSGroup(fakeGroupMemberAddRequest.getSocietyId(), "some-society-email@nobroker.in", utilsMethods.getImposterEmail());

        // now add a member to it
        mockMvc.perform(post("/group/member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json().build().writeValueAsString(fakeGroupMemberAddRequest)))
                .andExpect(status().isOk());

        //clean up delete member and group
        groupMemberService.removeMemberFromTJSGroupAndGoogleGroup(fakeGroupMemberAddRequest.getSocietyId(), fakeGroupMemberAddRequest.getMemberEmail(), fakeGroupMemberAddRequest.getFirebaseUserId());
        groupService.deleteGroup(fakeGroupMemberAddRequest.getSocietyId());
    }

    @Test
    public void canHandleGroupMemberRemoval() throws Exception {
        logger.info("Running test for  group member removal");
        TJSGroup fakeTJSGroup = Instancio.create(TJSGroup.class);
        fakeTJSGroup = groupService.createTJSGroup(fakeTJSGroup.getSocietyId(), "some-email-2@norboker.in", utilsMethods.getImposterEmail());
        TJSGroupMember fakeTJSGroupMember = Instancio.create(TJSGroupMember.class);
        fakeTJSGroupMember.setSocietyId(fakeTJSGroup.getSocietyId());
        fakeTJSGroupMember = groupMemberService.addMemberToTJSGroup(fakeTJSGroupMember.getSocietyId(), fakeTJSGroupMember.getMemberEmail(), fakeTJSGroupMember.getFirebaseUserId());

        GroupMemberDeleteRequest groupMemberDeleteRequest = Instancio.create(GroupMemberDeleteRequest.class);
        groupMemberDeleteRequest.setMemberEmail(fakeTJSGroupMember.getMemberEmail());
        groupMemberDeleteRequest.setSocietyId(fakeTJSGroupMember.getSocietyId());
        groupMemberDeleteRequest.setFirebaseUserId(fakeTJSGroupMember.getFirebaseUserId());

        mockMvc.perform(post("/group/member/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json().build().writeValueAsString(groupMemberDeleteRequest)))
                .andExpect(status().isOk());
        // clean up groups
        groupService.deleteGroup(fakeTJSGroup.getSocietyId());
    }
}
