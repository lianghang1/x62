package com.scaffold.search.planpck;

import cn.zhxu.bs.bean.DbField;
import cn.zhxu.bs.bean.SearchBean;

/**
 * 普通 @SearchBean 检索实体（联表）。
 */
@SearchBean(tables = "t_user u left join t_dept d on u.dept_id = d.id")
public class UserVO {

    @DbField("u.id")
    private Long id;

    @DbField("u.name")
    private String name;

    @DbField("u.age")
    private Integer age;

    @DbField("u.status")
    private Integer status;

    @DbField("d.dept_name")
    private String deptName;

    @DbField("u.deleted_at")
    private java.util.Date deletedAt;

    public java.util.Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(java.util.Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
}
