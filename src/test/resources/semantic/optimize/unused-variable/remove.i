var global is 5
routine t(p : integer) is
    var unused : integer
    unused := 3
    var text is "unused"
    var first is 2
    var second is first
    var used is p
    print used
    for i in 1..2 loop
        var dead is 8
        print i
    end
end
