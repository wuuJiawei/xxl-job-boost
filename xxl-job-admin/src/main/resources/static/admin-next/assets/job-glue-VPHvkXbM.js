var e=`GLUE code initialized by Admin Next`,t={GLUE_GROOVY:`package com.xxl.job.service.handler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;

public class DemoGlueJobHandler extends IJobHandler {

    @Override
    public void execute() throws Exception {
        XxlJobHelper.log("XXL-JOB, Hello World.");
    }
}
`,GLUE_SHELL:`#!/bin/bash
echo "xxl-job: hello shell"

echo "script location: $0"
echo "executor param: $1"
echo "shard index: $2"
echo "shard total: $3"

echo "Good bye!"
exit 0
`,GLUE_PYTHON:`#!/usr/bin/python3
# -*- coding: UTF-8 -*-
import sys

print("xxl-job: hello python")
print("script location:", sys.argv[0])
print("executor param:", sys.argv[1])
print("shard index:", sys.argv[2])
print("shard total:", sys.argv[3])

print("Good bye!")
`,GLUE_PYTHON2:`#!/usr/bin/python
# -*- coding: UTF-8 -*-
import sys

print "xxl-job: hello python"
print "script location:", sys.argv[0]
print "executor param:", sys.argv[1]
print "shard index:", sys.argv[2]
print "shard total:", sys.argv[3]

print "Good bye!"
`,GLUE_PHP:`<?php

echo "xxl-job: hello php\\n";
echo "script location: $argv[0]\\n";
echo "executor param: $argv[1]\\n";
echo "shard index: $argv[2]\\n";
echo "shard total: $argv[3]\\n";

echo "Good bye!\\n";
exit(0);
`,GLUE_NODEJS:`#!/usr/bin/env node
console.log('xxl-job: hello nodejs');

const args = process.argv;
console.log('script location: ' + args[1]);
console.log('executor param: ' + args[2]);
console.log('shard index: ' + args[3]);
console.log('shard total: ' + args[4]);

console.log('Good bye!');
process.exit(0);
`,GLUE_POWERSHELL:`Write-Host "xxl-job: hello powershell"

Write-Host "script location: " $MyInvocation.MyCommand.Definition
Write-Host "executor param: "
if ($args.Count -gt 2) { $args[0..($args.Count-3)] }
Write-Host "shard index: " $args[$args.Count-2]
Write-Host "shard total: " $args[$args.Count-1]

Write-Host "Good bye!"
exit 0
`},n={GLUE_GROOVY:`text/x-java`,GLUE_SHELL:`text/x-sh`,GLUE_PYTHON:`text/x-python`,GLUE_PYTHON2:`text/x-python`,GLUE_PHP:`application/x-httpd-php`,GLUE_NODEJS:`text/javascript`,GLUE_POWERSHELL:`application/x-powershell`};function r(e){return e.startsWith(`GLUE_`)}function i(e){return t[e]||``}function a(e){return n[e]||`text/plain`}export{r as i,a as n,i as r,e as t};