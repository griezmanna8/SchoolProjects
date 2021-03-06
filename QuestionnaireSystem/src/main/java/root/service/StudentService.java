package root.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import root.Util.StringAndListUtil;
import root.dao.NaireDao;
import root.dao.StudentDao;
import root.model.Naire;
import root.model.ResCount;
import root.model.Student;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Vector;

@Service
public class StudentService {
    /**
     * 学生最大选课数量, 默认为5门
     */
    private int maxSubject;
    /**
     * 学生对象、问卷对象、问卷数据库对象及，学生数据库对象
     */
    private Student student = new Student();
    private Naire naire = new Naire();

    @Autowired
    private StudentDao studentDao;

    @Autowired
    private NaireDao naireDao;

    @PostConstruct
    public void init(){
        maxSubject = 5;
    }

    /**
     * 用来获取需要催缴问卷的课程列表
     */
    public List<String> getPressSubjectList(){
        System.out.println("into stu");
        /**
         * 保存naireDao中IsPress为1的科目列表
         */
        List<String> pressSubjectList = new Vector<String>();
        /**
         * 保存最后该student需要催缴的科目列表
         */
        List<String> pressSubjectResList = new Vector<String>();
        /**
         * 拿到所选的课程，首先判断naireDao
         * 中subject对应的IsPress是否为0，
         * 如果为0，则不用，否则需要催缴
         */
        List<String> subjectListTmp = studentDao.getSubjects(student);
        if (subjectListTmp == null || subjectListTmp.size() == 0 || subjectListTmp.get(0).equals("")) {
            return null;
        }

        for(String str : subjectListTmp) {
            naire.setSubject(str);
            if(naireDao.getPress(naire).equals("1")){
                pressSubjectList.add(str);
            }
        }

        for(String str : pressSubjectList){
            System.out.println(str);
        }

        /**
         * 根据pressSubjectList，去naireDao中根据课程名拿到
         * 未完成的学生名单与之相比，如果姓名与自己相同，则需要
         * 进行催缴，保存到pressSubjectResList
         */
        for(String str : pressSubjectList) {
            naire.setSubject(str);
            List<String> uncompletesListTmp = naireDao.getUncompletes(naire);
            if (uncompletesListTmp == null || uncompletesListTmp.size() == 0 || uncompletesListTmp.get(0).equals("")) {
                continue;
            }
            if(uncompletesListTmp.indexOf(student.getName()) != -1){
                pressSubjectResList.add(str);
            }
        }
        return pressSubjectResList;
    }

    /**
     * 学生登陆
     */
    public boolean Login(String id, String password){
        student.setId(id);
        student.setPassword(password);
        if((student = studentDao.isExist(student)) == null)
            return false;
        return true;
    }

    /**
     * 获取可选择的课程
     * @return
     */
    public List<String> getCanChooseSubject() {
        List<String> subjects = naireDao.getAllSubjects();
        List<String> student_subject = studentDao.getSubjects(student);
        if (student_subject == null) {
            return subjects;
        }
        List<String> canChoose = new Vector<>();
        for (String subject : subjects) {
            for (String choose : student_subject) {
                if (subject.equals(choose)) {
                    break;
                }
                if (!subject.equals(choose) &&
                        choose.equals(student_subject.get(student_subject.size() - 1))) {
                    canChoose.add(subject);
                }
            }
        }
        return canChoose;
    }

    /**
     * 添加课程
     * @param //item为课程名称
     */
    public boolean AddSubject(String item){
        /**
         * 从数据库拿到对应学生的subjectList
         */
        student.setId(student.getId());
        List<String> subjectsListTmp = studentDao.getSubjects(student);
        if (subjectsListTmp == null || subjectsListTmp.size() == 0 ||  subjectsListTmp.get(0).equals("")) {
            subjectsListTmp = new Vector<>();
        }

        List<String> useList = new Vector<>();
        for (String tmp : subjectsListTmp) {
            useList.add(tmp);
        }


        if(useList.size() == maxSubject)
            return false;

        /**
         * 是否已经添加该课程
         */
        if(useList.indexOf(item) != -1)
            return false;

        useList.add(item);
        student.setSubjects(useList);
        /**
         * 添加成功后更新到数据库
         */
        studentDao.UpdateSubjects(student);
        /**
         * 还要将学生姓名添加到naireDao中
         * 首先，拿到naireDao中的学生姓名List
         * 然后将该学生姓名添加到List，最后将
         * List更新到naireDao中
         */
        naire.setSubject(item);
        List<String> listTmp = naireDao.getStudents(naire);
        if (listTmp == null || listTmp.size() == 0 || listTmp.get(0).equals("")) {
            listTmp = new Vector<>();
        }
        useList.clear();
        for (String tmp : listTmp) {
            useList.add(tmp);
        }
        useList.add(student.getName());
        naire.setStudents(useList);
        naireDao.UpdateStudents(naire);

        /**
         * 也需要跟新naireDao中的未完成学生名单
         */
        listTmp = naireDao.getUncompletes(naire);
        if (listTmp == null || listTmp.size() == 0 || listTmp.get(0).equals("")) {
            listTmp = new Vector<>();
        }
        useList.clear();
        for (String str : listTmp) {
            useList.add(str);
        }
        useList.add(student.getName());
        naire.setUncompletes(useList);
        naireDao.UpdateUncompletes(naire);

        return true;
    }

    /**
     * 删除科目
     * @param //item表示科目名称
     */
    public boolean DelCourse(String item){
        /**
         * 从数据库拿到对应学生的subjectList
         */
        student.setId(student.getId());
        List<String> subjectsListTmp = studentDao.getSubjects(student);
        if (subjectsListTmp == null || subjectsListTmp.size() == 0 || subjectsListTmp.get(0).equals("")) {
            return true;
        }

        Integer i = subjectsListTmp.indexOf(item);
        if(i != -1){
            subjectsListTmp = StringAndListUtil.deleteStr(subjectsListTmp, item);
            student.setSubjects(subjectsListTmp);
            /**
             * 删除成功后更新到数据库
             */
            studentDao.UpdateSubjects(student);
            /**
             * 还要将学生姓名从naireDao中删除
             * 首先，拿到naireDao中的学生姓名List
             * 然后将该学生姓名从List中删除，最后将
             * List更新到naireDao中
             */
            naire.setSubject(item);
            List<String> listTmp = naireDao.getStudents(naire);
            if (listTmp == null || listTmp.size() == 0 || listTmp.get(0).equals("")) {
                return false;
            }
            listTmp = StringAndListUtil.deleteStr(listTmp, student.getName());
            naire.setStudents(listTmp);
            naireDao.UpdateStudents(naire);

            /**
             * 也需要跟新naireDao中的未完成学生名单
             */
            listTmp = naireDao.getUncompletes(naire);
            if (listTmp == null || listTmp.size() == 0 || listTmp.get(0).equals("")) {
                return false;
            }
            listTmp = StringAndListUtil.deleteStr(listTmp, student.getName());
            naire.setUncompletes(listTmp);
            naireDao.UpdateUncompletes(naire);
            return true;
        }
        /**
         * 否则，删除失败
         */
        return false;
    }

    public boolean isExist() { return student != null;}

    /**
     * 查看学生所选课程
     */
    public  List<String> getSubjects(){
        /**
         * 从数据库拿到对应学生的subjectList
         */
        List<String> subjectsListTmp = studentDao.getSubjects(student);

        return subjectsListTmp;
    }

    public void test() {
       naire.setSubject("chinese");
       naireDao.UpdateQuestions(naire);
    }

    /**
     * 查看某一科问卷
     * @param item //科目名称
     */
    public List<String> getNaire(String item){
        naire.setSubject(item);
        /**
         * 从数据库拉取问卷信息
         * 保存在成员变量naire中
         */
        String ques = naireDao.getQuestionnaires(naire);
        if (ques == null || ques == "") {
            return null;
        }
        return StringAndListUtil.strToList(ques);
    }

    /**
     * 查看结果
     * 这里返回该学生所选的某一门课程
     * 对应问卷的结果
     */
    public List<ResCount> getSutdentNaireRes(String item){
        List<ResCount> resCountList = TeacherService.getResultCount(item);
        return resCountList;
    }

    /**
     * 回答问题后学生需要进行的操作
     * 1.将naireDao中该student的name从uncomplete中删除
     * 2.再拿到该学生的已选科目，然后将这些科目在naireDao
     *   中找到对应的uncomplete，若有该学生名单，则添加到
     *   resList中。
     */
    public List<String> AnswerQuestion(String subject){
        /**
         * 1.将naireDao中该student的name从uncomplete中删除
         */
        List<String> resList = new Vector<String>();
        Naire naireTmp = new Naire();
        naireTmp.setSubject(subject);
        List<String> unList = naireDao.getUncompletes(naireTmp);
        if(!(unList == null || unList.size() == 0 || unList.get(0) == "")){
            unList = StringAndListUtil.deleteStr(unList, student.getName());
        }

        naireTmp.setUncompletes(unList);
        naireDao.UpdateUncompletes(naireTmp);

        /**
         * 2.再拿到该学生的已选科目，然后将这些科目在naireDao
         *   中找到对应的uncomplete，若有该学生名单，则添加到
         *    resList中。
         */
        List<String> subListTmp = studentDao.getSubjects(student);


        for(String str : subListTmp){
            Naire naTmp = new Naire();
            naTmp.setSubject(str);
            List<String> unComList = naireDao.getUncompletes(naTmp);
            if(null == unComList || 0 == unComList.size() || "" == unComList.get(0)){
                continue;
            }
            for(String na : unComList){

                if(na.equals(student.getName())){
                    resList.add(str);
                    break;
                }
            }
        }

        return resList;
    }

    public List<String> UNComplete() {
        List<String> resList = new Vector<String>();
        List<String> subListTmp = studentDao.getSubjects(student);
        ;

        for (String str : subListTmp) {
            Naire naTmp = new Naire();
            naTmp.setSubject(str);
            List<String> unComList = naireDao.getUncompletes(naTmp);
            if (null == unComList || 0 == unComList.size() || "" == unComList.get(0)) {
                continue;
            }
            for (String na : unComList) {

                if (na.equals(student.getName())) {
                    resList.add(str);
                    break;
                }
            }
        }
        return resList;
    }



}


