type Point is record
    var x: integer
    var y: integer
end

routine norm(p: Point): integer is
    return p.x * p.x + p.y * p.y
end

routine main() is
    var p: Point
    var values: array [3] integer
    p.x := 3
    p.y := 4
    for i in 1..3 loop
        values[i] := norm(p) + i
    end
    for v in values loop
        print v
    end
end
