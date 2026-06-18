package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.UserActivity;
import com.vcsm.repository.UserActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @InjectMocks
    private UserActivityService userActivityService;

    @Test
    void testGetUserActivities_Paginated() {
        User user = new User();
        user.setId(1L);
        
        Pageable pageable = PageRequest.of(0, 10);
        UserActivity activity = new UserActivity(user, "COMPLAINT", "Filed complaint", null);
        Page<UserActivity> expectedPage = new PageImpl<>(Collections.singletonList(activity), pageable, 1);

        when(userActivityRepository.findByUser(user, pageable)).thenReturn(expectedPage);

        Page<UserActivity> result = userActivityService.getUserActivities(user, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("COMPLAINT", result.getContent().get(0).getActionType());
        verify(userActivityRepository, times(1)).findByUser(user, pageable);
    }
}
