package com.example;

import java.io.IOException;

public class MyClass {

    public static void main(String []s) throws IOException, InterruptedException {
       // p(""+(int)'\t'+"AA");

        DictGraph d= DictGraph.get();
        d.loadDict("xxx.txt");
        d.loadSource("test.txt");
        DictGraph.DictItem x;
        while ((x=d.getOne()).classId!=-1)
        {
            p(x.toString());
        }
    }

    public static void p(String s)
    {
        System.out.println(s);
    }
    static int a=0;
    static class A implements Runnable
    {

        private void aaa()
        {
            p("xxx");
        }

        @Override
        public void run() {
            a++;
            x(a);
            aaa();
        }


    }

    static class B extends A{

    }
    private static void x(int i)
    {
        p("aaaaaaaaaaaaaa"+i);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
