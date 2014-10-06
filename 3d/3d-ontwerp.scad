difference() {
	union() {
		cylinder(h=120, r=34);
		translate([0, 0, 120]) cylinder(h=1, r=36);
	}
	translate([0, 0, 2]) cylinder(h=120, r=32.5);
	for(i = [1:45:180]) {
		rotate(i) translate([-2.5, -50, 100]) cube([5, 100, 15]);
	}
}