package io.vanguard.testops.system.mapper;

import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.dto.CommentUserInfo;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.dto.user.UserDTO;
import io.vanguard.testops.system.dto.user.UserExcludeOptionDTO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface BaseUserMapper {
    UserDTO selectDTOByKeyword(String keyword);

    UserDTO selectById(String id);

    String selectNameById(String id);

    List<User> findAll();

    void batchSave(@Param("users") List<User> users);

    boolean isSuperUser(String userId);

    String selectEmailInDB(@Param("email") String email, @Param("id") String id);

    List<User> selectUserIdByEmailList(@Param("emailList") Collection<String> emailList);

    List<User> selectByKeyword(@Param("keyword") String keyword, @Param("selectId") boolean selectId);

    List<String> selectUnDeletedUserIdByIdList(@Param("idList") List<String> userIdList);

    long deleteUser(String id, String deleteUser, long deleteTime);

    List<OptionDTO> selectUserOptionByIds(List<String> userIds);

    /**
     * 按用户 id 或 email 查询用户选项（用于负责人等展示 name；create_user 可能存 id 或 email）
     */
    List<UserExcludeOptionDTO> selectUserOptionByIdOrEmail(List<String> idsOrEmails);

    List<UserExcludeOptionDTO> getExcludeSelectOptionWithLimit(@Param("keyword") String keyword);

    List<OptionDTO> getSelectOptionByIdsWithDeleted(List<String> ids);

    /**
     * 获取评论用户信息
     *
     * @param ids 用户ID集合
     * @return 评论用户信息集合
     */
    List<CommentUserInfo> getCommentUserInfoByIds(List<String> ids);

    /**
     * 获取开启的项目和组织
     */
    Project getEnableProjectAndOrganization();

}
