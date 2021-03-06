package com.se.tss.infomgr.api;

import com.alibaba.fastjson.JSONObject;
import com.se.tss.infomgr.annotation.CurrentUser;
import com.se.tss.infomgr.annotation.LoginRequired;
import com.se.tss.infomgr.config.UploadConfig;
import com.se.tss.infomgr.model.*;
import com.se.tss.infomgr.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/user")
public class UserApi {
    private UserService userService;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UploadConfig uploadConfig;

    @Autowired
    public UserApi(UserService userService, UserRepository userRepository, StudentRepository studentRepository, TeacherRepository teacherRepository, UploadConfig uploadConfig) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.uploadConfig = uploadConfig;
    }

    @PostMapping("/add")
    public Object add(@RequestBody User user) {
        if (userRepository.findByName(user.getName()) != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "用户名已被使用");
            return jsonObject;
        }
        return userService.add(user);
    }

    @GetMapping("/info")
    @LoginRequired
    public Object findById(@CurrentUser User currentUser) {
        return userRepository.getOne(currentUser.getId());
    }

    @PostMapping("/update")
    @LoginRequired
    public Object userInfo(@RequestBody JSONObject request) {
        User userInDB = userRepository.findByName(request.getString("name"));
        JSONObject jsonObject = new JSONObject();
        if (userInDB == null) {
            jsonObject.put("error", "用户不存在");
        } else {
            userInDB.setAge(request.getInteger("age"));
            userInDB.setGender(Gender.valueOf(request.getString("gender")));
            userInDB.setPhone(request.getString("phone"));
            if (userInDB.getClass().equals(Student.class)) {
                Student student = (Student) userInDB;
                student.setClassNum(request.getInteger("classNum"));
                student.setDepartment(request.getString("department"));
                student.setGrade(request.getInteger("grade"));
                student.setMajor(request.getString("major"));
                studentRepository.save(student);
            } else if (userInDB.getClass().equals(Teacher.class)) {
                Teacher teacher = (Teacher) userInDB;
                teacher.setDepartment(request.getString("department"));
                teacher.setTitle(request.getString("title"));
                teacherRepository.save(teacher);
            } else {
                userRepository.save(userInDB);
            }
            jsonObject.put("message", "success");
        }
        return jsonObject;
    }

    @PostMapping("/uploadImage")
    @LoginRequired
    public Object uploadImage(@RequestParam(value = "image") MultipartFile file, @CurrentUser User currentUser) {
        JSONObject jsonObject = new JSONObject();
        if (file.isEmpty()) {
            jsonObject.put("error", "文件不能为空");
            return jsonObject;
        }
        File dest = new File(uploadConfig.getUploadPath() + currentUser.getId() + ".jpg");
        try {
            file.transferTo(dest);
            jsonObject.put("message", "success");
            return jsonObject;
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }
        jsonObject.put("error", "文件上传失败");
        return jsonObject;
    }
}