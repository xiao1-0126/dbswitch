package com.gitee.dbswitch.admin.convert;

import com.gitee.dbswitch.admin.entity.AssignmentTaskEntity;
import com.gitee.dbswitch.admin.model.response.AssignmentsDataResponse;

/**
 * @author Li Zemin
 * @since 2024/4/22 9:44
 */
//@Mapper
public interface AssignmentConvert {
	AssignmentsDataResponse toAssignmentsDataResponse(AssignmentTaskEntity assignmentTaskEntity);
}
